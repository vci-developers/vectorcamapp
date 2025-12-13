package com.vci.vectorcamapp.registration.logging

import android.os.Build
import com.vci.vectorcamapp.core.logging.Crashy
import com.vci.vectorcamapp.core.logging.CrashyContext

object RegistrationSentryLogger {

    fun logProgramNotFound(e: Exception) {
        Crashy.exception(
            throwable = e,
            context = CrashyContext(
                screen = "Registration",
                feature = "ProgramSelection",
                action = "program_not_found"
            ),
            tags = mapOf(
                "error_type" to "program_not_found",
                "critical" to "true",
                "user_blocking" to "true"
            ),
            extras = mapOf(
                "error_context" to "Program selected by the user could not be found",
                "recovery_action" to "User may need to re-register device",
                "possible_causes" to "Program removed from database"
            )
        )
    }

    fun logDeviceRegistrationFailure(e: Exception, failedProgramId: Int) {
        Crashy.exception(
            throwable = e,
            context = CrashyContext(
                screen = "Registration",
                feature = "DeviceRegistration",
                action = "device_registration_failure",
                programId = failedProgramId.toString()
            ),
            tags = mapOf(
                "error_type" to "device_registration_failure",
                "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "critical" to "true",
                "user_blocking" to "true"
            ),
            extras = mapOf(
                "requested_program_id" to failedProgramId,
                "recovery_action" to "User may need to re-register device",
                "registration_timestamp" to System.currentTimeMillis(),
                "possible_causes" to "Device cache corrupted"
            )
        )
    }
}
