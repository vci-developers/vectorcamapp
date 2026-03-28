package com.vci.vectorcamapp.core.domain.model

import android.net.Uri
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.model.CameraMetadata

data class SpecimenImage constructor(
    val localId: String,
    val remoteId: Int?,
    val species: String?,
    val sex: String?,
    val abdomenStatus: String?,
    val imageUri: Uri,
    val metadataUploadStatus: UploadStatus,
    val imageUploadStatus: UploadStatus,
    val capturedAt: Long,
    val submittedAt: Long?,
    val imageMetadata: CameraMetadata?,
)
