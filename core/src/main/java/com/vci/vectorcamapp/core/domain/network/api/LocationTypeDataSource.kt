package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.location_type.GetAllLocationTypesResponseDto
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError

interface LocationTypeDataSource {
    suspend fun getAllLocationTypesForProgram(programId: Int): Result<GetAllLocationTypesResponseDto, NetworkError>
}
