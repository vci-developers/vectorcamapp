package com.vci.vectorcamapp.core.data.network

import io.ktor.http.HttpStatusCode

internal enum class ResumeDisposition {
    /** Body continues at the requested offset. Append it. */
    APPEND,

    /** Body starts at byte 0. Replace the partial file with it. */
    RESTART,

    /** Body cannot be placed at the requested offset. Discard it and retry from 0. */
    REJECT,
}

internal object ByteContentRange {
    private val START_BYTE = Regex("""bytes\s+(\d+)-\d+/(?:\d+|\*)""", RegexOption.IGNORE_CASE)

    /** First byte of a `Content-Range` value, or null when the header is missing or not a byte range. */
    fun startByte(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        return START_BYTE.find(header.trim())?.groupValues?.getOrNull(1)?.toLongOrNull()
    }
}

/**
 * Decides whether a resumed download body continues the partial file.
 *
 * A 200, or a 206 whose range starts at 0, ignored the requested offset. A 206 must name the
 * requested start; anything else is a gap or an overlap and must not be appended.
 */
internal fun resumeDisposition(
    statusCode: Int,
    contentRange: String?,
    startByte: Long,
): ResumeDisposition {
    if (startByte <= 0L) return ResumeDisposition.APPEND
    if (statusCode == HttpStatusCode.OK.value) return ResumeDisposition.RESTART
    if (statusCode != HttpStatusCode.PartialContent.value) return ResumeDisposition.REJECT
    return when (ByteContentRange.startByte(contentRange)) {
        startByte -> ResumeDisposition.APPEND
        0L -> ResumeDisposition.RESTART
        else -> ResumeDisposition.REJECT
    }
}
