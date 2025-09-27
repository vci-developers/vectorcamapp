package com.vci.vectorcamapp.core.domain.model.helpers

data class SessionUploadProgress(
    val uploadedImageCount: Int,
    val totalImageCount: Int,
    val isUploading: Boolean
)
