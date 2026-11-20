package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.session.PostSessionResponseDto
import com.vci.vectorcamapp.core.data.dto.session.SessionDto
import com.vci.vectorcamapp.core.data.mappers.toPostSessionRequestDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.network.api.SessionDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import java.util.UUID
import javax.inject.Inject

class RemoteSessionDataSource @Inject constructor(
    private val httpClient: HttpClient
) : SessionDataSource {

    override suspend fun postSession(
        session: Session, siteId: Int, deviceId: Int, expectedSpecimens: Int
    ): Result<PostSessionResponseDto, NetworkError> {
        if (session.completedAt == null) {
            return Result.Error(NetworkError.SESSION_NOT_COMPLETED)
        }

        return safeCall<PostSessionResponseDto> {
            httpClient.post(constructUrl(UploadRoutes.SESSIONS)) {
                setBody(session.toPostSessionRequestDto(siteId, deviceId, expectedSpecimens))
            }
        }
    }

    override suspend fun getSessionByFrontendId(localId: UUID): Result<SessionDto, NetworkError> {
        return safeCall<SessionDto> {
            httpClient.get(constructUrl("sessions/$localId"))
        }
    }
}
