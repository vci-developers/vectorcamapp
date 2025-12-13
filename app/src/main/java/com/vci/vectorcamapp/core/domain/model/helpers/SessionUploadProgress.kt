package com.vci.vectorcamapp.core.domain.model.helpers

data class SessionUploadProgress(
    val uploadedMetadataCount: Int,
    val uploadedImageCount: Int,
    val totalCount: Int,
    val failedImageCount: Int,
    val isUploading: Boolean
)
