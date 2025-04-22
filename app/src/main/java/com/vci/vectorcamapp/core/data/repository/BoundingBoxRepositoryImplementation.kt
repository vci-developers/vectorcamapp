package com.vci.vectorcamapp.core.data.repository

import com.vci.vectorcamapp.core.data.mappers.toEntity
import com.vci.vectorcamapp.core.data.room.dao.BoundingBoxDao
import com.vci.vectorcamapp.core.domain.model.BoundingBox
import com.vci.vectorcamapp.core.domain.repository.BoundingBoxRepository
import javax.inject.Inject

class BoundingBoxRepositoryImplementation @Inject constructor(
    private val boundingBoxDao: BoundingBoxDao
) : BoundingBoxRepository {
    override suspend fun upsertBoundingBox(boundingBox: BoundingBox, specimenId: String): Boolean {
        return boundingBoxDao.upsertBoundingBox(boundingBox.toEntity(specimenId)) != -1L
    }
}