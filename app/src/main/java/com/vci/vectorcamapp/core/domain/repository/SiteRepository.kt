package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow

interface SiteRepository {
    suspend fun upsertSite(
        site: Site,
        programId: Int,
        locationTypeId: Int?,
        parentId: Int?
    ): Result<Unit, RoomDbError>

    fun observeAllSitesByProgramId(programId: Int): Flow<List<Site>>
    suspend fun getSiteById(id: Int): Site?
    suspend fun setAllSitesInactiveForProgram(programId: Int)
}
