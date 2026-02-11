package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.site.GetAllSitesResponseDto
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError

interface SiteDataSource {
    suspend fun getSitesForProgram(programId: Int): Result<GetAllSitesResponseDto, NetworkError>
}
