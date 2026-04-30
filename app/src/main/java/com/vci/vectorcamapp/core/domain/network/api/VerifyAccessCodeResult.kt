package com.vci.vectorcamapp.core.domain.network.api

import com.vci.vectorcamapp.core.domain.util.network.NetworkError

sealed interface VerifyAccessCodeResult {
    data object Valid : VerifyAccessCodeResult
    data class Invalid(val message: String) : VerifyAccessCodeResult
    data class Failed(val error: NetworkError) : VerifyAccessCodeResult
}
