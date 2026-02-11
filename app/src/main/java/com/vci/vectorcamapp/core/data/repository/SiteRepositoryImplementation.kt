package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.room.TransactionHelper
import com.vci.vectorcamapp.core.data.room.dao.SiteDao
import com.vci.vectorcamapp.core.data.room.entities.SiteEntity
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import com.vci.vectorcamapp.core.domain.util.Result
import com.vci.vectorcamapp.core.domain.util.room.RoomDbError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SiteRepositoryImplementation @Inject constructor(
    private val siteDao: SiteDao,
    private val transactionHelper: TransactionHelper
) : SiteRepository {

    override fun observeAllSitesByProgramId(programId: Int): Flow<List<Site>> {
        return siteDao.observeAllSitesByProgramId(programId).map { siteEntities -> siteEntities.map { it.toDomain() } }
    }

    override suspend fun getSiteById(id: Int): Site? {
        return siteDao.getSiteById(id)?.toDomain()
    }

    override suspend fun upsertAllSites(siteEntities: List<SiteEntity>): Result<Unit, RoomDbError> {
        return try {
            if (siteEntities.isEmpty()) return Result.Success(Unit)

            val ids = siteEntities.map { it.id }.toHashSet()

            transactionHelper.runAsTransaction {
                val pass1 = siteEntities.map { it.copy(parentId = null) }
                siteDao.upsertAllSites(pass1)

                siteEntities.forEach { e ->
                    val parent = e.parentId
                    if (parent != null && parent in ids) {
                        siteDao.updateParentId(siteId = e.id, parentId = parent)
                    }
                }
            }

            Result.Success(Unit)
        } catch (_: Exception) {
            Result.Error(RoomDbError.UNKNOWN_ERROR)
        }
    }
}
