package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.SiteDao
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SiteRepositoryImplementation @Inject constructor(
    private val siteDao: SiteDao,
) : SiteRepository {

    override suspend fun upsertSite(site: Site, programId: Int, locationTypeId: Int?, parentId: Int?): Result<Unit, RoomDbError> {
        return try {
            siteDao.upsertSite(site.toEntity(programId, locationTypeId, parentId))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }

    override fun observeAllSitesByProgramId(programId: Int): Flow<List<Site>> {
        return siteDao.observeAllSitesByProgramId(programId).map { siteEntities -> siteEntities.map { it.toDomain() } }
    }

    override suspend fun getSiteById(id: Int): Site? {
        return siteDao.getSiteById(id)?.toDomain()
    }
}
