package com.vci.vectorcamapp.surveillance_form.location.data

import com.vci.vectorcamapp.core.domain.util.Error
import java.io.ObjectStreamException

sealed class LocationError : Error {
    abstract val message: String

    object PermissionDenied : LocationError() {
        override val message = "Permission denied"

        @Throws(ObjectStreamException::class)
        private fun readResolve(): Any = PermissionDenied
    }

    object Timeout : LocationError() {
        override val message = "GPS timeout"

        @Throws(ObjectStreamException::class)
        private fun readResolve(): Any = Timeout
    }

    data class Unknown(override val message: String) : LocationError()
}
