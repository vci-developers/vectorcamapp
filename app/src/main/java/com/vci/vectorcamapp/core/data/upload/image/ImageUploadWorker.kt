package com.vci.vectorcamapp.core.data.upload.image

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.domain.model.Specimen
import com.vci.vectorcamapp.core.domain.model.SpecimenImage
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.tus.java.client.TusClient
import io.tus.java.client.TusUpload
import io.tus.java.client.TusUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import androidx.work.ListenableWorker.Result as WorkerResult
import com.vci.vectorcamapp.core.domain.util.Result as DomainResult
import io.tus.java.client.ProtocolException as TusProtocolException

@HiltWorker
class ImageUploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val specimenRepository: SpecimenRepository,
    private val specimenImageRepository: SpecimenImageRepository,
    private val sessionRepository: SessionRepository,
    private val tusClient: TusClient
) : CoroutineWorker(context, workerParams) {

    private data class ImageUploadTask(val specimen: Specimen, val image: SpecimenImage)

    companion object {
        const val KEY_SESSION_ID = "session_id"

        const val KEY_PROGRESS_UPLOADED = "progress_uploaded"
        const val KEY_PROGRESS_TOTAL = "progress_total"

        private const val INITIAL_CHUNK_SIZE_BYTES = 64 * 1024
        private const val MIN_CHUNK_SIZE_BYTES = 16 * 1024
        private const val MAX_CHUNK_SIZE_BYTES = 1024 * 1024
        private const val SUCCESS_STREAK_FOR_INCREASE = 5
        private const val SUCCESS_CHUNK_SIZE_MULTIPLIER = 2
        private const val FAILURE_CHUNK_SIZE_DIVIDER = 2

        private const val MAX_RETRIES = 5

        private const val CHANNEL_ID = "image_upload_channel"
        private const val CHANNEL_NAME = "Image Upload Channel"

        private const val COMPRESSION_QUALITY = 30
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var notificationId: Int = 1002
    private var notificationSessionTitle: String = ""
    private var notificationTotalImages: Int = 0
    private var notificationCurrentImageIndex: Int = 0

    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    override suspend fun doWork(): WorkerResult {
        val sessionIdStr = inputData.getString(KEY_SESSION_ID)
        if (sessionIdStr == null) {
            Timber.tag("ImageUploadWorker").e("Session ID missing from worker input data.")
            return WorkerResult.failure()
        }

        val sessionId = try {
            UUID.fromString(sessionIdStr)
        } catch (e: IllegalArgumentException) {
            Timber.tag("ImageUploadWorker")
                .e(e, "Invalid session ID format provided: $sessionIdStr")
            return WorkerResult.failure()
        }

        val session = sessionRepository.getSessionById(sessionId)
        if (session == null) {
            Timber.tag("ImageUploadWorker").e("Session $sessionId not found in the database.")
            return WorkerResult.failure()
        }

        val specimensWithImages =
            specimenRepository.getSpecimenImagesAndInferenceResultsBySession(sessionId)

        val imagesToUpload = specimensWithImages.flatMap { specimenWithData ->
            specimenWithData.specimenImagesAndInferenceResults.map { imageAndResult ->
                ImageUploadTask(specimenWithData.specimen, imageAndResult.specimenImage)
            }
        }.filter { task ->
            task.image.imageUploadStatus != UploadStatus.COMPLETED
        }

        if (imagesToUpload.isEmpty()) {
            Timber.tag("ImageUploadWorker").d("No images to upload for session $sessionId.")
            return WorkerResult.success()
        }

        createNotificationChannel()
        val sessionDateStr = dateFormatter.format(Date(session.createdAt))
        notificationSessionTitle = "Session from $sessionDateStr"
        notificationId = sessionId.hashCode()
        setForeground(showInitialSessionNotification(imagesToUpload.size))

        var successfulUploads = 0
        var encounteredPermanentFailure = false

        imagesToUpload.forEachIndexed { index, task ->
            notificationTotalImages = imagesToUpload.size
            notificationCurrentImageIndex = index + 1

            if (task.image.remoteId == null) {
                Timber.tag("ImageUploadWorker")
                    .e("Image ${task.image.localId} has a null remoteId. Skipping.")
                specimenImageRepository.updateSpecimenImage(
                    specimenImage = task.image.copy(imageUploadStatus = UploadStatus.FAILED),
                    specimenId = task.specimen.id,
                    sessionId = sessionId
                )
                encounteredPermanentFailure = true
                return@forEachIndexed
            }

            when (val result = uploadSingleImage(task, sessionId)) {
                is DomainResult.Success -> {
                    updateProgressNotification("Verifying...")
                    successfulUploads++
                }

                is DomainResult.Error -> {
                    if (result.error == NetworkError.TUS_PERMANENT_ERROR) {
                        encounteredPermanentFailure = true
                    }
                }
            }
        }

        showFinalStatusNotification(successfulUploads, imagesToUpload.size)
        return if (successfulUploads == imagesToUpload.size) {
            WorkerResult.success()
        } else if (encounteredPermanentFailure) {
            WorkerResult.failure()
        } else {
            WorkerResult.retry()
        }
    }

    private suspend fun uploadSingleImage(
        task: ImageUploadTask,
        sessionId: UUID
    ): DomainResult<String, NetworkError> {
        var tempFile: File? = null
        val (file, contentType, md5) = try {
            val (prepared, type) = prepareFile(task.image.imageUri, task.specimen.id, task.image.localId)
            tempFile = prepared
            Triple(prepared, type, task.image.localId)
        } catch (e: Exception) {
            if (isStopped) {
                throw CancellationException("Worker was stopped during file preparation.", e)
            }
            Timber.tag("ImageUploadWorker")
                .e(e, "Failed to prepare file or calculate MD5 for specimen ${task.image.localId}.")
            specimenImageRepository.updateSpecimenImage(
                specimenImage = task.image.copy(imageUploadStatus = UploadStatus.FAILED),
                specimenId = task.specimen.id,
                sessionId = sessionId
            )
            tempFile?.takeIf { it.exists() }?.delete()
            return DomainResult.Error(NetworkError.TUS_PERMANENT_ERROR)
        }

        val metadata = mapOf(
            "filename" to file.name,
            "contentType" to contentType,
            "filemd5" to md5,
            "imageId" to task.image.remoteId.toString(),
        )

        val uploadResult: DomainResult<String, NetworkError> = run {
            for (attempt in 1..MAX_RETRIES) {
                val result: DomainResult<String, NetworkError> = try {
                    val uniqueFingerprint = "${task.specimen.remoteId}-${task.image.localId}-$md5"
                    val tusPath = "specimens/${task.specimen.remoteId}/images/tus"

                    tusClient.uploadCreationURL = URL(constructUrl(tusPath))
                    val upload = createTusUpload(file, uniqueFingerprint, metadata)
                    Timber.tag("ImageUploadWorker")
                        .d("Attempt $attempt: Start/resume ${file.name} (fp=$uniqueFingerprint,md5=$md5)")

                    val uploader: TusUploader = try {
                        val uploaderInstance = tusClient.resumeOrCreateUpload(upload)
                        specimenImageRepository.updateSpecimenImage(
                            specimenImage = task.image.copy(imageUploadStatus = UploadStatus.IN_PROGRESS),
                            specimenId = task.specimen.id,
                            sessionId = sessionId
                        )
                        Timber.tag("ImageUploadWorker")
                            .d("Connection established for ${task.image.localId}.")
                        uploaderInstance
                    } catch (e: TusProtocolException) {
                        if (e.causingConnection?.responseCode == HttpURLConnection.HTTP_CONFLICT) {
                            val location = e.causingConnection.getHeaderField("Location")
                            if (location != null) {
                                Timber.tag("ImageUploadWorker")
                                    .i("Upload for ${task.image.localId} already exists on server (conflict). Treating as success.")
                                specimenImageRepository.updateSpecimenImage(
                                    specimenImage = task.image.copy(imageUploadStatus = UploadStatus.COMPLETED),
                                    specimenId = task.specimen.id,
                                    sessionId = sessionId
                                )
                                return@run DomainResult.Success(location)
                            }
                        }
                        file.delete()
                        throw e
                    }

                    when (val loopResult = executeUploadLoop(uploader, upload, file)) {
                        is DomainResult.Error -> loopResult
                        is DomainResult.Success -> {
                            when (val finalUrlResult = safeFinish(uploader, file)) {
                                is DomainResult.Success -> {
                                    Timber.tag("ImageUploadWorker")
                                        .d("Upload finished successfully: ${finalUrlResult.data}")
                                    DomainResult.Success(finalUrlResult.data.toString())
                                }

                                is DomainResult.Error -> finalUrlResult
                            }
                        }
                    }
                } catch (e: TusProtocolException) {
                    Timber.tag("ImageUploadWorker")
                        .w(e, "Attempt $attempt failed with TusProtocolException.")
                    if (e.shouldRetry()) {
                        DomainResult.Error(NetworkError.TUS_TRANSIENT_ERROR)
                    } else {
                        DomainResult.Error(NetworkError.TUS_PERMANENT_ERROR)
                    }
                } catch (e: IOException) {
                    Timber.tag("ImageUploadWorker")
                        .w(e, "Attempt $attempt failed with IOException.")
                    DomainResult.Error(NetworkError.TUS_TRANSIENT_ERROR)
                } catch (e: Exception) {
                    if (isStopped) {
                        file.delete()
                        throw CancellationException("Worker was stopped by the system.", e)
                    }
                    Timber.tag("ImageUploadWorker")
                        .e(e, "Exception on attempt $attempt for specimen ${task.image.localId}.")
                    return@run DomainResult.Error(NetworkError.UNKNOWN_ERROR)
                }

                if (result is DomainResult.Success) {
                    Timber.tag("ImageUploadWorker")
                        .d("Success for specimen ${task.image.localId} on attempt $attempt")
                    return@run result
                }

                result as DomainResult.Error<NetworkError>
                Timber.tag("ImageUploadWorker")
                    .w("Failed attempt $attempt for specimen ${task.image.localId} with error: ${result.error}")

                if (result.error == NetworkError.TUS_PERMANENT_ERROR || attempt == MAX_RETRIES) {
                    if (result.error == NetworkError.TUS_PERMANENT_ERROR) {
                        Timber.tag("ImageUploadWorker")
                            .e("Permanent error for specimen ${task.image.localId}.")
                    } else {
                        Timber.tag("ImageUploadWorker")
                            .e("Max retries reached for specimen ${task.image.localId}.")
                    }
                    return@run result
                }
            }
            DomainResult.Error(NetworkError.UNKNOWN_ERROR)
        }


        val finalStatus = if (uploadResult is DomainResult.Success) {
            UploadStatus.COMPLETED
        } else {
            UploadStatus.FAILED
        }

        if (uploadResult is DomainResult.Success) {
            try {
                Timber.tag("ImageUploadWorker")
                    .d("Upload successful. Compressing original file: ${task.image.imageUri}")
                compressImageFile(
                    task.image.imageUri,
                    COMPRESSION_QUALITY,
                    specimenId = task.specimen.id,
                    imageId = task.image.localId
                )
            } catch (e: Exception) {
                Timber.tag("ImageUploadWorker")
                    .e(e, "Failed to compress original file (${task.image.imageUri}) after upload.")
            }
        }

        specimenImageRepository.updateSpecimenImage(
            specimenImage = task.image.copy(imageUploadStatus = finalStatus),
            specimenId = task.specimen.id,
            sessionId = sessionId
        )
        file.delete()
        return uploadResult
    }

    private suspend fun executeUploadLoop(
        initialUploader: TusUploader,
        upload: TusUpload,
        file: File,
    ): DomainResult<Unit, NetworkError> {
        var uploader = initialUploader
        var currentChunkSize = INITIAL_CHUNK_SIZE_BYTES
        var successfulUploadsInARow = 0
        var recoveryAttempts = 0

        while (uploader.offset < upload.size) {
            uploader.chunkSize = currentChunkSize
            uploader.requestPayloadSize = currentChunkSize

            val chunkResult = try {
                DomainResult.Success(uploader.uploadChunk())
            } catch (_: SocketTimeoutException) {
                DomainResult.Error(NetworkError.TUS_TRANSIENT_ERROR)
            } catch (e: TusProtocolException) {
                if (e.shouldRetry()) {
                    DomainResult.Error(NetworkError.TUS_TRANSIENT_ERROR)
                } else {
                    DomainResult.Error(NetworkError.TUS_PERMANENT_ERROR)
                }
            } catch (_: IOException) {
                DomainResult.Error(NetworkError.TUS_TRANSIENT_ERROR)
            } catch (e: Exception) {
                if (isStopped) {
                    file.delete()
                    throw CancellationException("Worker was stopped during chunk upload.", e)
                }
                DomainResult.Error(NetworkError.UNKNOWN_ERROR)
            }

            when (chunkResult) {
                is DomainResult.Success -> {
                    val bytesUploaded = chunkResult.data
                    if (bytesUploaded <= -1) {
                        break
                    }
                    val percent =
                        if (upload.size > 0) (uploader.offset * 100 / upload.size).toInt() else 0
                    updateProgressNotification("$percent%")
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS_UPLOADED to uploader.offset,
                            KEY_PROGRESS_TOTAL to upload.size
                        )
                    )

                    successfulUploadsInARow++
                    recoveryAttempts = 0
                    if (successfulUploadsInARow >= SUCCESS_STREAK_FOR_INCREASE) {
                        currentChunkSize =
                            (currentChunkSize * SUCCESS_CHUNK_SIZE_MULTIPLIER).coerceAtMost(
                                MAX_CHUNK_SIZE_BYTES
                            )
                        Timber.tag("ImageUploadWorker")
                            .i("Increasing chunk size to $currentChunkSize bytes.")
                        successfulUploadsInARow = 0
                    }
                }

                is DomainResult.Error -> {
                    val error = chunkResult.error
                    successfulUploadsInARow = 0
                    currentChunkSize =
                        (currentChunkSize / FAILURE_CHUNK_SIZE_DIVIDER).coerceAtLeast(
                            MIN_CHUNK_SIZE_BYTES
                        )

                    if (error == NetworkError.TUS_PERMANENT_ERROR) {
                        return DomainResult.Error(error)
                    }

                    recoveryAttempts++
                    if (recoveryAttempts >= MAX_RETRIES) {
                        Timber.tag("ImageUploadWorker")
                            .e("Exceeded max recovery attempts for a single image upload. Failing.")
                        return DomainResult.Error(error)
                    }

                    try {
                        uploader = tusClient.resumeOrCreateUpload(upload)
                    } catch (resumeException: Exception) {
                        if (isStopped) {
                            file.delete()
                            throw CancellationException(
                                "Worker was stopped by the system.",
                                resumeException
                            )
                        }
                        Timber.tag("ImageUploadWorker")
                            .e(resumeException, "Failed to resume upload.")
                        return DomainResult.Error(NetworkError.UNKNOWN_ERROR)
                    }
                }
            }
        }
        Timber.tag("ImageUploadWorker").d("Upload loop finished. Final offset: ${uploader.offset}")
        return DomainResult.Success(Unit)
    }

    private fun createTusUpload(
        file: File,
        fingerprint: String,
        metadata: Map<String, String>
    ): TusUpload {
        return TusUpload(file).apply {
            this.fingerprint = fingerprint
            this.metadata = metadata
        }
    }

    private suspend fun prepareFile(
        source: Uri,
        specimenId: String,
        imageId: String
    ): Pair<File, String> =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(source)
            val extension = when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                else -> "bin"
            }
            val filename = "upload_${specimenId}_${imageId}.$extension"
            val destination = File(context.cacheDir, filename)
            if (!destination.exists()) {
                resolver.openInputStream(source)?.use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("Unable to open $source")
            }
            return@withContext Pair(destination, mimeType ?: "application/octet-stream")
        }

    private suspend fun safeFinish(
        uploader: TusUploader,
        file: File
    ): DomainResult<URL, NetworkError> =
        withContext(Dispatchers.IO) {
            try {
                uploader.finish()
                Timber.tag("ImageUploadWorker").d("Tus finish() successful.")
                DomainResult.Success(uploader.uploadURL)
            } catch (e: TusProtocolException) {
                Timber.tag("ImageUploadWorker").w(e, "finish() failed with TusProtocolException.")
                if (e.shouldRetry()) {
                    DomainResult.Error(NetworkError.TUS_TRANSIENT_ERROR)
                } else {
                    DomainResult.Error(NetworkError.TUS_PERMANENT_ERROR)
                }
            } catch (e: IOException) {
                Timber.tag("ImageUploadWorker").e(e, "finish() failed due to IOException.")
                DomainResult.Error(NetworkError.TUS_TRANSIENT_ERROR)
            } catch (e: Exception) {
                if (isStopped) {
                    file.delete()
                    throw CancellationException("Worker was stopped during Tus finish().", e)
                }
                Timber.tag("ImageUploadWorker").e(e, "finish() failed due to unexpected exception.")
                DomainResult.Error(NetworkError.UNKNOWN_ERROR)
            }
        }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    private fun showInitialSessionNotification(total: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(notificationSessionTitle)
            .setContentText("Preparing to upload $total images…")
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setProgress(total, 0, true)
            .setOngoing(true)
            .build()
        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun updateProgressNotification(progressText: String) {
        val filesCompleted = notificationCurrentImageIndex - 1

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(notificationSessionTitle)
            .setContentText("Uploading image $notificationCurrentImageIndex of $notificationTotalImages")
            .setSubText(progressText)
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setProgress(notificationTotalImages, filesCompleted, false)
            .setOngoing(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private fun showFinalStatusNotification(successful: Int, total: Int) {
        val title = if (successful == total) "Upload complete" else "Upload error"
        val message = "$notificationSessionTitle: $successful of $total images uploaded."
        val icon = if (successful == total) R.drawable.ic_cloud_upload else R.drawable.ic_info

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    private suspend fun compressImageFile(
        uri: Uri,
        quality: Int,
        specimenId: String,
        imageId: String
    ) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var bitmap: Bitmap? = null
        var backupFile: File? = null

        try {
            backupFile =
                File.createTempFile("img_backup_${specimenId}_${imageId}", ".tmp", context.cacheDir)

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(backupFile).use { out ->
                    input.copyTo(out)
                }
            } ?: throw IOException("Failed to open input stream for $uri to create backup")

            bitmap = backupFile.inputStream().use { backupIn ->
                BitmapFactory.decodeStream(backupIn)
            } ?: throw IOException("Failed to decode bitmap from backup for $uri")

            resolver.openOutputStream(uri, "wt")?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                    throw IOException("Failed to compress bitmap to $uri")
                }
                out.flush()
            } ?: throw IOException("Failed to open output stream for $uri")

            Timber.tag("ImageUploadWorker").d("Successfully compressed $uri to $quality% quality.")
        } catch (e: Exception) {
            Timber.tag("ImageUploadWorker")
                .e(e, "Error compressing $uri, attempting to restore original")
            if (backupFile != null && backupFile.exists()) {
                try {
                    resolver.openOutputStream(uri, "wt")?.use { out ->
                        backupFile.inputStream().use { backupIn ->
                            backupIn.copyTo(out)
                        }
                        out.flush()
                    }
                    Timber.tag("ImageUploadWorker")
                        .d("Successfully restored original image from backup for $uri")
                } catch (restore: Exception) {
                    Timber.tag("ImageUploadWorker")
                        .e(restore, "Failed to restore original from backup for $uri")
                }
            }
            throw e
        } finally {
            bitmap?.recycle()
            backupFile?.delete()
        }
    }
}
