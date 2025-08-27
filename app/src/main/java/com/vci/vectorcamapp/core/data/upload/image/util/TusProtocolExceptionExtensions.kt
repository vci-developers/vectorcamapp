package com.vci.vectorcamapp.core.data.upload.image.util

import io.tus.java.client.ProtocolException as TusProtocolException
import java.net.HttpURLConnection

/**
 * Extension functions for TusProtocolException to handle retry logic and upload verification.
 */

/**
 * Determines if a TusProtocolException should be retried based on the HTTP status code.
 * 
 * @return true if the error is transient and should be retried, false for permanent errors
 */
fun TusProtocolException.shouldRetry(): Boolean {
    val responseCode = this.causingConnection?.responseCode ?: return false
    
    return when (responseCode) {
        // Transient errors that should be retried
        HttpURLConnection.HTTP_INTERNAL_ERROR,     // 500
        HttpURLConnection.HTTP_BAD_GATEWAY,        // 502
        HttpURLConnection.HTTP_UNAVAILABLE,        // 503
        HttpURLConnection.HTTP_GATEWAY_TIMEOUT,    // 504
        429, // Too Many Requests
        423, // Locked (TUS specific)
        -> true
        
        // Permanent errors that should not be retried
        HttpURLConnection.HTTP_BAD_REQUEST,        // 400
        HttpURLConnection.HTTP_UNAUTHORIZED,       // 401
        HttpURLConnection.HTTP_FORBIDDEN,          // 403
        HttpURLConnection.HTTP_NOT_FOUND,          // 404
        HttpURLConnection.HTTP_NOT_ACCEPTABLE,     // 406
        HttpURLConnection.HTTP_ENTITY_TOO_LARGE,   // 413
        HttpURLConnection.HTTP_UNSUPPORTED_TYPE,   // 415
        460, // Checksum Mismatch (TUS specific)
        -> false
        
        // Special case: HTTP_CONFLICT (409) needs special handling
        HttpURLConnection.HTTP_CONFLICT -> false
        
        // Default: retry for unknown status codes in 5xx range, fail for others
        else -> responseCode in 500..599
    }
}

/**
 * Checks if a TusProtocolException represents an upload conflict that might be completed.
 * 
 * @return true if this is an HTTP 409 conflict that might indicate a completed upload
 */
fun TusProtocolException.isUploadConflict(): Boolean {
    return this.causingConnection?.responseCode == HttpURLConnection.HTTP_CONFLICT
}

/**
 * Gets the location header from a TusProtocolException if available.
 * This is useful for HTTP 409 conflicts to get the existing upload URL.
 * 
 * @return the Location header value, or null if not available
 */
fun TusProtocolException.getLocationHeader(): String? {
    return this.causingConnection?.getHeaderField("Location")
}