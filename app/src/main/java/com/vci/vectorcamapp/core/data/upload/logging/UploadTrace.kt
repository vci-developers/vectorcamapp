package com.vci.vectorcamapp.core.data.upload.logging

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class UploadTrace(
    val worker: String,
) : AbstractCoroutineContextElement(Key) {
    var sessionId: String? = null
    var step: String = "start"
    var programId: String? = null
    var siteId: String? = null
    var specimenId: String? = null
    var imageId: String? = null
    var endpointHint: String? = null
    var requestBodyHint: String? = null
    var lastExchange: UploadHttpExchange? = null
    var failureCause: Throwable? = null

    companion object Key : CoroutineContext.Key<UploadTrace>
}
