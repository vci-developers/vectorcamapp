package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.data.room.entities.SiteEntity
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow

interface SiteRepository {
    fun observeAllSitesByProgramId(programId: Int): Flow<List<Site>>
    suspend fun getSiteById(id: Int): Site?
    suspend fun upsertAllSites(siteEntities: List<SiteEntity>): Result<Unit, RoomDbError>
}
