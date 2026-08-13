package com.vci.vectorcamapp.core.domain.util

/**
 * Error that surfaces an already-resolved user-facing message (e.g. from program config).
 */
data class MessageError(val message: String) : Error
