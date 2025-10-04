package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toDomain
import com.vci.vectorcamapp.core.data.room.dao.SiteDao
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.repository.SiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SiteRepositoryImplementation @Inject constructor(
    private val siteDao: SiteDao
) : SiteRepository {

    override fun observeAllSitesByProgramId(programId: Int): Flow<List<Site>> {
        return siteDao.observeAllSitesByProgramId(programId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getSiteById(id: Int): Site? {
        return siteDao.getSiteById(id)?.toDomain()
    }
}
