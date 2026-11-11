package com.vci.vectorcamapp.core.domain.model.enums

enum class UploadStatus {
    COMPLETED,
    IN_PROGRESS,
    NOT_STARTED,
    RECOVERED,
    FAILED;

    fun isUploaded(): Boolean = this == COMPLETED || this == RECOVERED
}
