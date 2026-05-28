package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.session_unit.PostSessionUnitResponseDto
import com.vci.vectorcamapp.core.data.dto.session_unit.SessionUnitDto
import com.vci.vectorcamapp.core.domain.model.SessionUnit
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError
import java.util.UUID

interface SessionUnitDataSource {
    suspend fun postSessionUnit(
        unit: SessionUnit,
        sessionRemoteId: Int,
    ): Result<PostSessionUnitResponseDto, NetworkError>

    suspend fun getSessionUnitByFrontendId(
        sessionRemoteId: Int,
        localId: UUID,
    ): Result<SessionUnitDto, NetworkError>
}
