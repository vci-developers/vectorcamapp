package com.vci.vectorcamapp.imaging.domain.repository

import android.graphics.Bitmap
import com.vci.vectorcamapp.imaging.domain.AbdomenStatusLabel
import com.vci.vectorcamapp.core.domain.model.BoundingBox
import com.vci.vectorcamapp.imaging.domain.SexLabel
import com.vci.vectorcamapp.imaging.domain.SpeciesLabel
import com.vci.vectorcamapp.imaging.presentation.model.BoundingBoxUi

interface InferenceRepository {
    suspend fun readSpecimenId(bitmap: Bitmap) : String

    suspend fun detectSpecimen(bitmap: Bitmap) : Pair<Bitmap, BoundingBox?>

    suspend fun classifySpecimen(bitmap: Bitmap) : Triple<SpeciesLabel?, SexLabel?, AbdomenStatusLabel?>

    fun convertToBoundingBox(boundingBoxUi: BoundingBoxUi?, imageWidth: Int, imageHeight: Int) : BoundingBox?

    fun convertToBoundingBoxUi(boundingBox: BoundingBox?, imageWidth: Int, imageHeight: Int) : BoundingBoxUi?

    fun closeResources()
}
