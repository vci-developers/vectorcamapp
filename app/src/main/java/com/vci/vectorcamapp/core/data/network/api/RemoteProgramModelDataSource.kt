package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.core.data.dto.program_model.GetProgramModelsResponseDto
import com.vci.vectorcamapp.core.data.dto.program_model.ProgramModelDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.responseToResult
import com.vci.vectorcamapp.core.domain.network.api.ProgramModelDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import com.vci.vectorcamapp.core.logging.ProgramModelLog
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.util.network.UnresolvedAddressException
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.coroutineContext

class RemoteProgramModelDataSource @Inject constructor(
    private val httpClient: HttpClient,
    @Named("ModelDownloadHttpClient") private val downloadHttpClient: HttpClient,
) : ProgramModelDataSource {

    override suspend fun getModels(programId: Int): Result<GetProgramModelsResponseDto, NetworkError> {
        val url = constructUrl("/programs/$programId/models")
        ProgramModelLog.i("API GET models list → %s", url)

        return try {
            val response = httpClient.get(url)
            ProgramModelLog.i(
                "API GET models list ← status=%d url=%s",
                response.status.value,
                response.request.url
            )
            val result = responseToResult<GetProgramModelsResponseDto>(response)
            when (result) {
                is Result.Success -> {
                    ProgramModelLog.i(
                        "API GET models list SUCCESS count=%d modelIds=%s",
                        result.data.models.size,
                        result.data.models.joinToString { it.modelId }
                    )
                }

                is Result.Error -> {
                    ProgramModelLog.w(
                        "API GET models list FAIL status=%d error=%s url=%s",
                        response.status.value,
                        result.error,
                        url
                    )
                }
            }
            result
        } catch (e: UnresolvedAddressException) {
            ProgramModelLog.e(e, "API GET models list FAIL NO_INTERNET url=%s", url)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (e: UnknownHostException) {
            ProgramModelLog.e(e, "API GET models list FAIL NO_INTERNET url=%s", url)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (ignored: Exception) {
            coroutineContext.ensureActive()
            ProgramModelLog.e(ignored, "API GET models list FAIL UNKNOWN url=%s", url)
            Result.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    override suspend fun getModel(programId: Int, modelId: String): Result<ProgramModelDto, NetworkError> {
        val url = constructUrl("/programs/$programId/models/$modelId")
        ProgramModelLog.i("API GET model metadata → %s", url)

        return try {
            val response = httpClient.get(url)
            ProgramModelLog.i(
                "API GET model metadata ← status=%d url=%s contentType=%s",
                response.status.value,
                response.request.url,
                response.headers[HttpHeaders.ContentType] ?: "n/a"
            )

            val result = responseToResult<ProgramModelDto>(response)
            when (result) {
                is Result.Success -> {
                    val model = result.data
                    ProgramModelLog.i(
                        "API GET model metadata SUCCESS id=%d programId=%d modelId=%s fileSize=%d fileMd5=%s downloadUrl=%s classes=%s",
                        model.id,
                        model.programId,
                        model.modelId,
                        model.fileSize,
                        model.fileMd5,
                        model.downloadUrl,
                        model.modelClasses.joinToString()
                    )
                }

                is Result.Error -> {
                    ProgramModelLog.w(
                        "API GET model metadata FAIL status=%d error=%s url=%s",
                        response.status.value,
                        result.error,
                        url
                    )
                }
            }
            result
        } catch (e: UnresolvedAddressException) {
            ProgramModelLog.e(e, "API GET model metadata FAIL NO_INTERNET url=%s", url)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (e: UnknownHostException) {
            ProgramModelLog.e(e, "API GET model metadata FAIL NO_INTERNET url=%s", url)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (ignored: Exception) {
            coroutineContext.ensureActive()
            ProgramModelLog.e(ignored, "API GET model metadata FAIL UNKNOWN url=%s", url)
            Result.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    override suspend fun downloadModel(
        downloadPath: String,
        destination: File,
        expectedSize: Long,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<Unit, NetworkError> {
        return try {
            withContext(Dispatchers.IO) {
                destination.parentFile?.mkdirs()

                var bytesDownloaded = if (destination.exists()) destination.length() else 0L
                if (bytesDownloaded > expectedSize) {
                    ProgramModelLog.w(
                        "Partial file oversized (%d > %d); deleting %s",
                        bytesDownloaded,
                        expectedSize,
                        destination.absolutePath
                    )
                    destination.delete()
                    bytesDownloaded = 0L
                }

                ProgramModelLog.i(
                    "S3 download begin apiPath=%s destination=%s resumeFrom=%d expectedSize=%d",
                    downloadPath,
                    destination.absolutePath,
                    bytesDownloaded,
                    expectedSize
                )
                onProgress(bytesDownloaded, expectedSize)

                val downloadResult = downloadUntilComplete(
                    downloadPath = downloadPath,
                    destination = destination,
                    expectedSize = expectedSize,
                    initialBytesDownloaded = bytesDownloaded,
                    onProgress = onProgress,
                )
                if (downloadResult is Result.Success) {
                    ProgramModelLog.i(
                        "S3 download SUCCESS bytes=%d destination=%s",
                        destination.length(),
                        destination.absolutePath
                    )
                }
                downloadResult
            }
        } catch (e: UnresolvedAddressException) {
            ProgramModelLog.e(e, "S3 download FAIL NO_INTERNET path=%s", downloadPath)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (e: UnknownHostException) {
            ProgramModelLog.e(e, "S3 download FAIL NO_INTERNET path=%s", downloadPath)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (ignored: Exception) {
            coroutineContext.ensureActive()
            ProgramModelLog.e(ignored, "S3 download FAIL UNKNOWN path=%s", downloadPath)
            Result.Error(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun downloadUntilComplete(
        downloadPath: String,
        destination: File,
        expectedSize: Long,
        initialBytesDownloaded: Long,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): Result<Unit, NetworkError> {
        var bytesDownloaded = initialBytesDownloaded
        var resumeAttempts = 0
        var lastLoggedPercent = -1
        var failure: NetworkError? = null

        while (failure == null && bytesDownloaded < expectedSize) {
            val urlResult = resolvePresignedUrl(downloadPath)
            if (urlResult is Result.Error) {
                ProgramModelLog.w(
                    "Resolve presigned URL FAIL error=%s apiPath=%s",
                    urlResult.error,
                    downloadPath
                )
                failure = urlResult.error
            } else {
                val chunkResult = downloadRangeChunk(
                    presignedUrl = (urlResult as Result.Success).data,
                    startByte = bytesDownloaded,
                    destination = destination,
                    expectedSize = expectedSize,
                    onProgress = { downloaded, total ->
                        onProgress(downloaded, total)
                        lastLoggedPercent = logProgressIfNeeded(downloaded, total, lastLoggedPercent)
                    },
                )
                failure = applyChunkResult(
                    chunkResult = chunkResult,
                    currentOffset = bytesDownloaded,
                    resumeAttempts = resumeAttempts,
                    onResumeAttempt = { resumeAttempts = it },
                    onBytesDownloaded = { bytesDownloaded = it },
                )
            }
        }

        return failure?.let { Result.Error(it) } ?: Result.Success(Unit)
    }

    private fun applyChunkResult(
        chunkResult: RangeDownloadResult,
        currentOffset: Long,
        resumeAttempts: Int,
        onResumeAttempt: (Int) -> Unit,
        onBytesDownloaded: (Long) -> Unit,
    ): NetworkError? {
        return when (chunkResult) {
            is RangeDownloadResult.ExpiredUrl -> {
                val nextAttempt = resumeAttempts + 1
                onResumeAttempt(nextAttempt)
                ProgramModelLog.w(
                    "Presigned URL expired/retryable attempt=%d/%d offset=%d",
                    nextAttempt,
                    MAX_RESUME_ATTEMPTS,
                    currentOffset
                )
                if (nextAttempt > MAX_RESUME_ATTEMPTS) {
                    ProgramModelLog.e("Resume attempts exhausted offset=%d", currentOffset)
                    NetworkError.UNKNOWN_ERROR
                } else {
                    null
                }
            }

            is RangeDownloadResult.Error -> {
                ProgramModelLog.w(
                    "S3 range download FAIL error=%s offset=%d",
                    chunkResult.error,
                    currentOffset
                )
                chunkResult.error
            }

            is RangeDownloadResult.Success -> {
                if (chunkResult.bytesDownloaded <= currentOffset) {
                    val nextAttempt = resumeAttempts + 1
                    onResumeAttempt(nextAttempt)
                    ProgramModelLog.w(
                        "S3 made no progress (still %d bytes); attempt=%d/%d",
                        currentOffset,
                        nextAttempt,
                        MAX_RESUME_ATTEMPTS
                    )
                    if (nextAttempt > MAX_RESUME_ATTEMPTS) {
                        NetworkError.UNKNOWN_ERROR
                    } else {
                        null
                    }
                } else {
                    onResumeAttempt(0)
                    onBytesDownloaded(chunkResult.bytesDownloaded)
                    null
                }
            }
        }
    }

    private fun logProgressIfNeeded(downloaded: Long, total: Long, lastLoggedPercent: Int): Int {
        if (total <= 0L) return lastLoggedPercent
        val percent = ((downloaded * 100) / total).toInt()
        if (percent < lastLoggedPercent + PROGRESS_LOG_STEP_PERCENT && percent != 100) {
            return lastLoggedPercent
        }
        ProgramModelLog.d("PROGRESS %d%% (%d / %d bytes)", percent, downloaded, total)
        return percent
    }

    private suspend fun resolvePresignedUrl(downloadPath: String): Result<String, NetworkError> {
        val url = constructUrl(downloadPath)
        ProgramModelLog.i("API GET download redirect → %s (followRedirects=false)", url)

        val response = try {
            downloadHttpClient.get(url) {
                bearerAuth(BuildConfig.VECTORCAM_API_KEY)
                timeout {
                    requestTimeoutMillis = RESOLVE_TIMEOUT_MS
                    socketTimeoutMillis = RESOLVE_TIMEOUT_MS
                }
            }
        } catch (ignored: Exception) {
            coroutineContext.ensureActive()
            return mapResolveFailure(ignored, url)
        }

        val location = response.headers[HttpHeaders.Location]
        ProgramModelLog.i(
            "API GET download redirect ← status=%d location=%s",
            response.status.value,
            location?.let { ProgramModelLog.redactUrl(it) } ?: "null"
        )

        if (isRedirectStatus(response.status.value)) {
            response.discardBody()
            return if (location.isNullOrBlank()) {
                ProgramModelLog.e("API redirect missing Location header url=%s", url)
                Result.Error(NetworkError.UNKNOWN_ERROR)
            } else {
                ProgramModelLog.i(
                    "API redirect SUCCESS presignedUrl=%s",
                    ProgramModelLog.redactUrl(location)
                )
                Result.Success(location)
            }
        }

        response.discardBody()
        val error = networkErrorForStatus(response.status.value) ?: NetworkError.UNKNOWN_ERROR
        ProgramModelLog.w(
            "API download redirect FAIL error=%s status=%d url=%s",
            error,
            response.status.value,
            url
        )
        return Result.Error(error)
    }

    private suspend fun downloadRangeChunk(
        presignedUrl: String,
        startByte: Long,
        destination: File,
        expectedSize: Long,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): RangeDownloadResult {
        val rangeHeader = if (startByte > 0L) "bytes=$startByte-" else null
        ProgramModelLog.i(
            "S3 GET → url=%s range=%s expectedSize=%d",
            ProgramModelLog.redactUrl(presignedUrl),
            rangeHeader ?: "none (full)",
            expectedSize
        )

        return try {
            downloadHttpClient.prepareGet(presignedUrl) {
                if (rangeHeader != null) {
                    header(HttpHeaders.Range, rangeHeader)
                }
                timeout {
                    requestTimeoutMillis = DOWNLOAD_TIMEOUT_MS
                    socketTimeoutMillis = DOWNLOAD_TIMEOUT_MS
                }
            }.execute { response ->
                ProgramModelLog.i(
                    "S3 GET ← status=%d contentLength=%s contentRange=%s acceptRanges=%s",
                    response.status.value,
                    response.contentLength()?.toString() ?: "n/a",
                    response.headers[HttpHeaders.ContentRange] ?: "n/a",
                    response.headers[HttpHeaders.AcceptRanges] ?: "n/a"
                )

                when (response.status.value) {
                    HttpStatusCode.Unauthorized.value,
                    HttpStatusCode.Forbidden.value,
                    -> {
                        response.discardBody()
                        ProgramModelLog.w(
                            "S3 GET expired/unauthorized status=%d",
                            response.status.value
                        )
                        RangeDownloadResult.ExpiredUrl
                    }

                    HttpStatusCode.OK.value,
                    HttpStatusCode.PartialContent.value,
                    -> {
                        ProgramModelLog.d(
                            "S3 GET body streaming status=%d startByte=%d",
                            response.status.value,
                            startByte
                        )
                        writeRangeBody(
                            response = response,
                            destination = destination,
                            startByte = startByte,
                            expectedSize = expectedSize,
                            onProgress = onProgress,
                        )
                    }

                    else -> {
                        response.discardBody()
                        val error = networkErrorForStatus(response.status.value)
                            ?: NetworkError.UNKNOWN_ERROR
                        ProgramModelLog.w(
                            "S3 GET FAIL error=%s status=%d",
                            error,
                            response.status.value
                        )
                        RangeDownloadResult.Error(error)
                    }
                }
            }
        } catch (e: UnresolvedAddressException) {
            ProgramModelLog.e(e, "S3 GET FAIL NO_INTERNET")
            RangeDownloadResult.Error(NetworkError.NO_INTERNET)
        } catch (e: UnknownHostException) {
            ProgramModelLog.e(e, "S3 GET FAIL NO_INTERNET")
            RangeDownloadResult.Error(NetworkError.NO_INTERNET)
        } catch (ignored: Exception) {
            coroutineContext.ensureActive()
            ProgramModelLog.e(
                ignored,
                "S3 GET stream interrupted; will resume from offset=%d",
                startByte
            )
            RangeDownloadResult.ExpiredUrl
        }
    }

    private suspend fun writeRangeBody(
        response: HttpResponse,
        destination: File,
        startByte: Long,
        expectedSize: Long,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): RangeDownloadResult {
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesDownloaded = startByte

        if (response.status == HttpStatusCode.OK && startByte > 0L) {
            ProgramModelLog.w(
                "S3 ignored Range (HTTP 200); restarting file from 0 (was offset=%d)",
                startByte
            )
            destination.delete()
            bytesDownloaded = 0L
        }

        RandomAccessFile(destination, "rw").use { raf ->
            raf.seek(bytesDownloaded)
            var keepReading = true
            while (keepReading && !channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    raf.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead
                    onProgress(bytesDownloaded.coerceAtMost(expectedSize), expectedSize)
                } else {
                    keepReading = bytesRead == 0
                }
            }
        }

        ProgramModelLog.d(
            "S3 body write finished bytesDownloaded=%d expectedSize=%d",
            bytesDownloaded,
            expectedSize
        )
        return RangeDownloadResult.Success(bytesDownloaded)
    }

    private suspend fun HttpResponse.discardBody() {
        runCatching { bodyAsChannel().cancel(null) }
    }

    private sealed interface RangeDownloadResult {
        data object ExpiredUrl : RangeDownloadResult
        data class Success(val bytesDownloaded: Long) : RangeDownloadResult
        data class Error(val error: NetworkError) : RangeDownloadResult
    }

    private fun mapResolveFailure(error: Exception, url: String): Result<String, NetworkError> {
        return when (error) {
            is UnresolvedAddressException, is UnknownHostException -> {
                ProgramModelLog.e(error, "API GET download redirect FAIL NO_INTERNET url=%s", url)
                Result.Error(NetworkError.NO_INTERNET)
            }
            else -> {
                ProgramModelLog.e(error, "API GET download redirect FAIL UNKNOWN url=%s", url)
                Result.Error(NetworkError.UNKNOWN_ERROR)
            }
        }
    }

    private fun isRedirectStatus(status: Int): Boolean {
        return status == HttpStatusCode.MovedPermanently.value ||
            status == HttpStatusCode.Found.value ||
            status == HttpStatusCode.SeeOther.value ||
            status == HttpStatusCode.TemporaryRedirect.value ||
            status == HttpStatusCode.PermanentRedirect.value
    }

    private fun networkErrorForStatus(status: Int): NetworkError? {
        return when (status) {
            HttpStatusCode.NotFound.value -> NetworkError.NOT_FOUND
            HttpStatusCode.Unauthorized.value -> NetworkError.CLIENT_ERROR
            HttpStatusCode.RequestTimeout.value -> NetworkError.REQUEST_TIMEOUT
            HttpStatusCode.TooManyRequests.value -> NetworkError.TOO_MANY_REQUESTS
            in HTTP_CLIENT_ERROR_MIN..HTTP_CLIENT_ERROR_MAX -> NetworkError.CLIENT_ERROR
            in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX -> NetworkError.SERVER_ERROR
            else -> null
        }
    }

    private companion object {
        const val RESOLVE_TIMEOUT_MS = 30_000L
        const val DOWNLOAD_TIMEOUT_MS = 10 * 60 * 1000L
        const val DEFAULT_BUFFER_SIZE = 8 * 1024
        const val MAX_RESUME_ATTEMPTS = 5
        const val PROGRESS_LOG_STEP_PERCENT = 5
        const val HTTP_CLIENT_ERROR_MIN = 400
        const val HTTP_CLIENT_ERROR_MAX = 499
        const val HTTP_SERVER_ERROR_MIN = 500
        const val HTTP_SERVER_ERROR_MAX = 599
    }
}
