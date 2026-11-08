package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryRequestDto
import com.vci.vectorcamapp.core.data.dto.data_recovery.PostDataRecoveryResponseDto
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError

interface DataRecoveryDataSource {
    suspend fun postDataRecovery(
        request: PostDataRecoveryRequestDto
    ): Result<PostDataRecoveryResponseDto, NetworkError>
}
