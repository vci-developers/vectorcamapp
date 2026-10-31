package com.vci.vectorcamapp.core.data.upload.logging

internal class UploadFailureException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
