package com.vci.vectorcamapp.core.data.network.api

import com.vci.vectorcamapp.core.data.dto.session_unit.GetSessionUnitsResponseDto
import com.vci.vectorcamapp.core.data.dto.session_unit.PostSessionUnitRequestDto
import com.vci.vectorcamapp.core.data.dto.session_unit.PostSessionUnitResponseDto
import com.vci.vectorcamapp.core.data.dto.session_unit.SessionUnitDto
import com.vci.vectorcamapp.core.data.network.constructUrl
import com.vci.vectorcamapp.core.data.network.safeCall
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.network.api.SessionUnitDataSource
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import java.util.UUID
import javax.inject.Inject

class RemoteSessionUnitDataSource @Inject constructor(
    private val httpClient: HttpClient
) : SessionUnitDataSource {

    override suspend fun postSessionUnit(
        sessionUnit: SessionUnit,
        sessionId: Int
    ): Result<PostSessionUnitResponseDto, NetworkError> {
        return safeCall<PostSessionUnitResponseDto> {
            httpClient.post(constructUrl("sessions/$sessionId/units")) {
                setBody(
                    PostSessionUnitRequestDto(
                        frontendId = sessionUnit.localId,
                        unitOrder = sessionUnit.unitOrder,
                        createdAt = sessionUnit.createdAt,
                    )
                )
            }
        }
    }

    override suspend fun getSessionUnitForSession(
        sessionId: Int
    ): Result<GetSessionUnitsResponseDto, NetworkError> {
        return safeCall<GetSessionUnitsResponseDto> {
            httpClient.get(constructUrl("sessions/$sessionId/units"))
        }
    }
}