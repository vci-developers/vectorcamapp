package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.Site
import kotlinx.coroutines.flow.Flow

interface SiteRepository {
    fun observeAllSitesByProgramId(programId: Int): Flow<List<Site>>
    suspend fun getSiteById(id: Int): Site?
}
