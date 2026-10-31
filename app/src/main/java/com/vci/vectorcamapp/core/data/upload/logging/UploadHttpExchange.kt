package com.vci.vectorcamapp.core.data.upload.logging

internal data class UploadHttpExchange(
    val method: String,
    val endpoint: String,
    val requestBody: String,
    val statusCode: Int?,
    val responseBody: String,
)
