package com.vci.vectorcamapp.imaging.domain.camera

import com.vci.vectorcamapp.core.domain.model.CameraMetadata

interface CameraMetadataListener {
    val latestMetadata: CameraMetadata?
    fun reset()
}
