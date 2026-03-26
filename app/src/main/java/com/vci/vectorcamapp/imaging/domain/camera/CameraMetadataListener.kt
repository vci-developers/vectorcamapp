package com.vci.vectorcamapp.imaging.domain.camera

import com.vci.vectorcamapp.imaging.domain.model.CameraMetadata

interface CameraMetadataListener {
    val latestMetadata: CameraMetadata?
    fun reset()
}
