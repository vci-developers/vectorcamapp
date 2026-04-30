package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.program.ApiErrorMessageDto
import com.vci.vectorcamapp.core.data.dto.program.GetAllProgramsResponseDto
import com.vci.vectorcamapp.core.data.dto.program.VerifyAccessCodeRequestDto
import com.vci.vectorcamapp.core.data.dto.program.VerifyAccessCodeResponseDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.network.api.ProgramDataSource
import com.vci.vectorcamapp.core.domain.network.api.VerifyAccessCodeResult
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext
import javax.inject.Inject

class RemoteProgramDataSource @Inject constructor(
    private val httpClient: HttpClient
) : ProgramDataSource {
    override suspend fun getAllPrograms(): Result<GetAllProgramsResponseDto, NetworkError> {
        return safeCall<GetAllProgramsResponseDto> {
            httpClient.get(constructUrl("programs"))
        }
    }

    override suspend fun verifyAccessCode(
        programId: Int,
        accessCode: String,
    ): VerifyAccessCodeResult {
        val response = try {
            httpClient.post(constructUrl("programs/$programId/verify-access-code")) {
                setBody(VerifyAccessCodeRequestDto(accessCode = accessCode))
            }
        } catch (e: UnresolvedAddressException) {
            return VerifyAccessCodeResult.Failed(NetworkError.NO_INTERNET)
        } catch (e: UnknownHostException) {
            return VerifyAccessCodeResult.Failed(NetworkError.NO_INTERNET)
        } catch (e: SerializationException) {
            return VerifyAccessCodeResult.Failed(NetworkError.SERIALIZATION_ERROR)
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            return VerifyAccessCodeResult.Failed(NetworkError.UNKNOWN_ERROR)
        }

        return when (response.status.value) {
            in 200..299 -> parseVerifySuccess(response)
            404 -> VerifyAccessCodeResult.Failed(NetworkError.NOT_FOUND)
            408 -> VerifyAccessCodeResult.Failed(NetworkError.REQUEST_TIMEOUT)
            409 -> VerifyAccessCodeResult.Failed(NetworkError.CONFLICT)
            429 -> VerifyAccessCodeResult.Failed(NetworkError.TOO_MANY_REQUESTS)
            in 400..499 -> parseVerifyClientError(response)
            in 500..599 -> VerifyAccessCodeResult.Failed(NetworkError.SERVER_ERROR)
            else -> VerifyAccessCodeResult.Failed(NetworkError.UNKNOWN_ERROR)
        }
    }

    private suspend fun parseVerifySuccess(response: HttpResponse): VerifyAccessCodeResult {
        return try {
            val body = response.body<VerifyAccessCodeResponseDto>()
            if (body.valid) {
                VerifyAccessCodeResult.Valid
            } else {
                VerifyAccessCodeResult.Invalid("Invalid access code")
            }
        } catch (e: NoTransformationFoundException) {
            VerifyAccessCodeResult.Failed(NetworkError.SERIALIZATION_ERROR)
        } catch (e: SerializationException) {
            VerifyAccessCodeResult.Failed(NetworkError.SERIALIZATION_ERROR)
        }
    }

    private suspend fun parseVerifyClientError(response: HttpResponse): VerifyAccessCodeResult {
        return try {
            val err = response.body<ApiErrorMessageDto>()
            VerifyAccessCodeResult.Invalid(err.error?.takeIf { it.isNotBlank() } ?: "Invalid access code")
        } catch (_: Exception) {
            VerifyAccessCodeResult.Failed(NetworkError.CLIENT_ERROR)
        }
    }
}
