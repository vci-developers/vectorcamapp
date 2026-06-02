package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.session_unit.GetSessionUnitsResponseDto
import com.vci.vectorcamapp.core.data.dto.session_unit.PostSessionUnitResponseDto
import com.vci.vectorcamapp.core.data.dto.session_unit.SessionUnitDto
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import java.util.UUID

interface SessionUnitDataSource {
    suspend fun postSessionUnit(
        sessionUnit: SessionUnit, sessionId: Int
    ): Result<PostSessionUnitResponseDto, NetworkError>

    suspend fun getSessionUnitForSession(
        sessionId: Int
    ): Result<GetSessionUnitsResponseDto, NetworkError>
}
