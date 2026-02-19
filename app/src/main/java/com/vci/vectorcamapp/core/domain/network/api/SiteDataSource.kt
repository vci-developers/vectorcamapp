package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.data.dto.site.SiteDto
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.network.NetworkError

interface SiteDataSource {
    suspend fun getAllSitesForProgram(programId: Int): Result<List<SiteDto>, NetworkError>
}
