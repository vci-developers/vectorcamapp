package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.BuildConfig
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

    override suspend fun getCurrentModel(programId: Int): Result<ProgramModelDto, NetworkError> {
        val url = constructUrl("/programs/$programId/models/current")
        ProgramModelLog.i("API GET metadata → %s", url)

        return try {
            val response = httpClient.get(url)
            ProgramModelLog.i(
                "API GET metadata ← status=%d url=%s contentType=%s",
                response.status.value,
                response.request.url,
                response.headers[HttpHeaders.ContentType] ?: "n/a"
            )

            val result = responseToResult<ProgramModelDto>(response)
            when (result) {
                is Result.Success -> {
                    val model = result.data
                    ProgramModelLog.i(
                        "API GET metadata SUCCESS id=%d programId=%d version=%s fileSize=%d fileMd5=%s downloadUrl=%s classes=%s",
                        model.id,
                        model.programId,
                        model.version,
                        model.fileSize,
                        model.fileMd5,
                        model.downloadUrl,
                        model.modelClasses.joinToString()
                    )
                }

                is Result.Error -> {
                    ProgramModelLog.w(
                        "API GET metadata FAIL status=%d error=%s url=%s",
                        response.status.value,
                        result.error,
                        url
                    )
                }
            }
            result
        } catch (e: UnresolvedAddressException) {
            ProgramModelLog.e(e, "API GET metadata FAIL NO_INTERNET url=%s", url)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (e: UnknownHostException) {
            ProgramModelLog.e(e, "API GET metadata FAIL NO_INTERNET url=%s", url)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            ProgramModelLog.e(e, "API GET metadata FAIL UNKNOWN url=%s", url)
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

                var resumeAttempts = 0
                var lastLoggedPercent = -1
                while (bytesDownloaded < expectedSize) {
                    when (val urlResult = resolvePresignedUrl(downloadPath)) {
                        is Result.Error -> {
                            ProgramModelLog.w(
                                "Resolve presigned URL FAIL error=%s apiPath=%s",
                                urlResult.error,
                                downloadPath
                            )
                            return@withContext Result.Error(urlResult.error)
                        }

                        is Result.Success -> {
                            when (
                                val chunkResult = downloadRangeChunk(
                                    presignedUrl = urlResult.data,
                                    startByte = bytesDownloaded,
                                    destination = destination,
                                    expectedSize = expectedSize,
                                    onProgress = { downloaded, total ->
                                        onProgress(downloaded, total)
                                        if (total > 0L) {
                                            val percent = ((downloaded * 100) / total).toInt()
                                            if (percent >= lastLoggedPercent + 5 || percent == 100) {
                                                lastLoggedPercent = percent
                                                ProgramModelLog.d(
                                                    "PROGRESS %d%% (%d / %d bytes)",
                                                    percent,
                                                    downloaded,
                                                    total
                                                )
                                            }
                                        }
                                    },
                                )
                            ) {
                                is RangeDownloadResult.ExpiredUrl -> {
                                    resumeAttempts++
                                    ProgramModelLog.w(
                                        "Presigned URL expired/retryable attempt=%d/%d offset=%d",
                                        resumeAttempts,
                                        MAX_RESUME_ATTEMPTS,
                                        bytesDownloaded
                                    )
                                    if (resumeAttempts > MAX_RESUME_ATTEMPTS) {
                                        ProgramModelLog.e(
                                            "Resume attempts exhausted offset=%d",
                                            bytesDownloaded
                                        )
                                        return@withContext Result.Error(NetworkError.UNKNOWN_ERROR)
                                    }
                                }

                                is RangeDownloadResult.Error -> {
                                    ProgramModelLog.w(
                                        "S3 range download FAIL error=%s offset=%d",
                                        chunkResult.error,
                                        bytesDownloaded
                                    )
                                    return@withContext Result.Error(chunkResult.error)
                                }

                                is RangeDownloadResult.Success -> {
                                    if (chunkResult.bytesDownloaded <= bytesDownloaded) {
                                        resumeAttempts++
                                        ProgramModelLog.w(
                                            "S3 made no progress (still %d bytes); attempt=%d/%d",
                                            bytesDownloaded,
                                            resumeAttempts,
                                            MAX_RESUME_ATTEMPTS
                                        )
                                        if (resumeAttempts > MAX_RESUME_ATTEMPTS) {
                                            return@withContext Result.Error(NetworkError.UNKNOWN_ERROR)
                                        }
                                    } else {
                                        resumeAttempts = 0
                                        bytesDownloaded = chunkResult.bytesDownloaded
                                        ProgramModelLog.d(
                                            "S3 chunk success bytesDownloaded=%d expectedSize=%d",
                                            bytesDownloaded,
                                            expectedSize
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ProgramModelLog.i(
                    "S3 download SUCCESS bytes=%d destination=%s",
                    bytesDownloaded,
                    destination.absolutePath
                )
                Result.Success(Unit)
            }
        } catch (e: UnresolvedAddressException) {
            ProgramModelLog.e(e, "S3 download FAIL NO_INTERNET path=%s", downloadPath)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (e: UnknownHostException) {
            ProgramModelLog.e(e, "S3 download FAIL NO_INTERNET path=%s", downloadPath)
            Result.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            ProgramModelLog.e(e, "S3 download FAIL UNKNOWN path=%s", downloadPath)
            Result.Error(NetworkError.UNKNOWN_ERROR)
        }
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
        } catch (e: UnresolvedAddressException) {
            ProgramModelLog.e(e, "API GET download redirect FAIL NO_INTERNET url=%s", url)
            return Result.Error(NetworkError.NO_INTERNET)
        } catch (e: UnknownHostException) {
            ProgramModelLog.e(e, "API GET download redirect FAIL NO_INTERNET url=%s", url)
            return Result.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            ProgramModelLog.e(e, "API GET download redirect FAIL UNKNOWN url=%s", url)
            return Result.Error(NetworkError.UNKNOWN_ERROR)
        }

        val location = response.headers[HttpHeaders.Location]
        ProgramModelLog.i(
            "API GET download redirect ← status=%d location=%s",
            response.status.value,
            location?.let { ProgramModelLog.redactUrl(it) } ?: "null"
        )

        return when (response.status.value) {
            HttpStatusCode.MovedPermanently.value,
            HttpStatusCode.Found.value,
            HttpStatusCode.SeeOther.value,
            HttpStatusCode.TemporaryRedirect.value,
            HttpStatusCode.PermanentRedirect.value,
            -> {
                response.discardBody()
                if (location.isNullOrBlank()) {
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

            404 -> {
                response.discardBody()
                ProgramModelLog.w("API download redirect FAIL NOT_FOUND url=%s", url)
                Result.Error(NetworkError.NOT_FOUND)
            }

            401 -> {
                response.discardBody()
                ProgramModelLog.w("API download redirect FAIL UNAUTHORIZED url=%s", url)
                Result.Error(NetworkError.CLIENT_ERROR)
            }

            408 -> {
                response.discardBody()
                ProgramModelLog.w("API download redirect FAIL TIMEOUT url=%s", url)
                Result.Error(NetworkError.REQUEST_TIMEOUT)
            }

            429 -> {
                response.discardBody()
                ProgramModelLog.w("API download redirect FAIL TOO_MANY_REQUESTS url=%s", url)
                Result.Error(NetworkError.TOO_MANY_REQUESTS)
            }

            in 400..499 -> {
                response.discardBody()
                ProgramModelLog.w(
                    "API download redirect FAIL CLIENT_ERROR status=%d url=%s",
                    response.status.value,
                    url
                )
                Result.Error(NetworkError.CLIENT_ERROR)
            }

            in 500..599 -> {
                response.discardBody()
                ProgramModelLog.w(
                    "API download redirect FAIL SERVER_ERROR status=%d url=%s",
                    response.status.value,
                    url
                )
                Result.Error(NetworkError.SERVER_ERROR)
            }

            else -> {
                response.discardBody()
                ProgramModelLog.w(
                    "API download redirect FAIL unexpected status=%d url=%s",
                    response.status.value,
                    url
                )
                Result.Error(NetworkError.UNKNOWN_ERROR)
            }
        }
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
                    401, 403 -> {
                        response.discardBody()
                        ProgramModelLog.w(
                            "S3 GET expired/unauthorized status=%d",
                            response.status.value
                        )
                        RangeDownloadResult.ExpiredUrl
                    }

                    200, 206 -> {
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

                    404 -> {
                        response.discardBody()
                        ProgramModelLog.w("S3 GET FAIL NOT_FOUND")
                        RangeDownloadResult.Error(NetworkError.NOT_FOUND)
                    }

                    408 -> {
                        response.discardBody()
                        ProgramModelLog.w("S3 GET FAIL TIMEOUT")
                        RangeDownloadResult.Error(NetworkError.REQUEST_TIMEOUT)
                    }

                    429 -> {
                        response.discardBody()
                        ProgramModelLog.w("S3 GET FAIL TOO_MANY_REQUESTS")
                        RangeDownloadResult.Error(NetworkError.TOO_MANY_REQUESTS)
                    }

                    in 400..499 -> {
                        response.discardBody()
                        ProgramModelLog.w("S3 GET FAIL CLIENT_ERROR status=%d", response.status.value)
                        RangeDownloadResult.Error(NetworkError.CLIENT_ERROR)
                    }

                    in 500..599 -> {
                        response.discardBody()
                        ProgramModelLog.w("S3 GET FAIL SERVER_ERROR status=%d", response.status.value)
                        RangeDownloadResult.Error(NetworkError.SERVER_ERROR)
                    }

                    else -> {
                        response.discardBody()
                        ProgramModelLog.w("S3 GET FAIL unexpected status=%d", response.status.value)
                        RangeDownloadResult.Error(NetworkError.UNKNOWN_ERROR)
                    }
                }
            }
        } catch (e: UnresolvedAddressException) {
            ProgramModelLog.e(e, "S3 GET FAIL NO_INTERNET")
            RangeDownloadResult.Error(NetworkError.NO_INTERNET)
        } catch (e: UnknownHostException) {
            ProgramModelLog.e(e, "S3 GET FAIL NO_INTERNET")
            RangeDownloadResult.Error(NetworkError.NO_INTERNET)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            ProgramModelLog.e(e, "S3 GET stream interrupted; will resume from offset=%d", startByte)
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
            while (!channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                if (bytesRead < 0) break
                if (bytesRead == 0) continue
                raf.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                onProgress(bytesDownloaded.coerceAtMost(expectedSize), expectedSize)
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

    private companion object {
        const val RESOLVE_TIMEOUT_MS = 30_000L
        const val DOWNLOAD_TIMEOUT_MS = 10 * 60 * 1000L
        const val DEFAULT_BUFFER_SIZE = 8 * 1024
        const val MAX_RESUME_ATTEMPTS = 5
    }
}
