package com.vci.vectorcamapp.core.domain.model.enums

enum class UploadStatus {
    COMPLETED,
    IN_PROGRESS,
    NOT_STARTED,
    RECOVERED,
    FAILED;

    fun isUploaded(): Boolean = this == COMPLETED || this == RECOVERED

    fun pendingIfInterrupted(): UploadStatus =
        if (this == IN_PROGRESS) NOT_STARTED else this
}
