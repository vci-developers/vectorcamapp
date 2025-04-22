package com.vci.vectorcamapp.core.domain.repository

import com.vci.vectorcamapp.core.domain.model.BoundingBox

interface BoundingBoxRepository {
    suspend fun upsertBoundingBox(boundingBox: BoundingBox, specimenId: String) : Boolean
}