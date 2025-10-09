package com.vci.vectorcamapp.intake.logging

import android.os.Build
import com.vci.vectorcamapp.core.logging.Crashy
import com.vci.vectorcamapp.core.logging.CrashyContext
import java.util.UUID

object IntakeSentryLogger {

    fun logProgramNotFound(e: Exception) {
        Crashy.exception(
            throwable = e,
            context = CrashyContext(
                screen = "Intake",
                feature = "ProgramDisplay",
                action = "program_not_found"
            ),
            tags = mapOf(
                "error_type" to "program_not_found",
                "critical" to "true",
                "user_blocking" to "true"
            ),
            extras = mapOf(
                "error_context" to "Program could not be found and displayed on the Intake Screen",
                "recovery_action" to "User may need to re-register device",
                "possible_causes" to "Program removed from database"
            )
        )
    }

    fun logSiteNotFound(e: Exception) {
        Crashy.exception(
            throwable = e,
            context = CrashyContext(
                screen = "Intake",
                feature = "SiteSelection",
                action = "site_not_found"
            ),
            tags = mapOf(
                "error_type" to "site_not_found",
                "critical" to "true",
                "user_blocking" to "true"
            ),
            extras = mapOf(
                "error_context" to "Site selected by user does not match any existing site",
                "recovery_action" to "User may need to restart device",
                "possible_causes" to "Site removed from database"
            )
        )
    }

    fun logSessionUpsertFailed(e: Exception, failedSessionId: UUID, failedSiteId: Int) {
        Crashy.exception(
            throwable = e,
            context = CrashyContext(
                screen = "Intake",
                feature = "SessionUpsert",
                action = "session_upsert_failed",
                sessionId = failedSessionId.toString(),
                siteId = failedSiteId.toString()
            ),
            tags = mapOf(
                "error_type" to "session_upsert_failed",
                "user_blocking" to "true",
                "critical" to "true"
            ),
            extras = mapOf(
                "error_context" to "Session cannot be upserted in the database",
                "requested_session_id" to failedSessionId,
                "requested_site_id" to failedSiteId,
                "recovery_action" to "User may need to resubmit intake form or restart the app",
                "possible_causes" to "Session table in database is corrupted or database migration failed"
            )
        )
    }

    fun logSurveillanceFormUpsertFailed(e: Exception, failedSessionId: UUID) {
        Crashy.exception(
            throwable = e,
            context = CrashyContext(
                screen = "Intake",
                feature = "SurveillanceFormUpsert",
                action = "surveillance_form_upsert_failed",
                sessionId = failedSessionId.toString()
            ),
            tags = mapOf(
                "error_type" to "surveillance_form_upsert_failed",
                "user_blocking" to "true",
                "critical" to "true"
            ),
            extras = mapOf(
                "error_context" to "Surveillance form cannot be upserted in the database",
                "requested_session_id" to failedSessionId,
                "recovery_action" to "User may need to resubmit intake form or restart the app",
                "possible_causes" to "Surveillance Form table in database is corrupted or database migration failed"
            )
        )
    }

    fun logLocationFetchFailed(e: Exception) {
        Crashy.exception(
            throwable = e,
            context = CrashyContext(
                screen = "Intake",
                action = "LocationFetch"
            ),
            tags = mapOf(
                "error_type" to "location_fetch_failed",
                "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}"
            ),
            extras = mapOf(
                "message" to "Location fetching failed for unknown reason",
                "details" to "Not related to GPS timeout or permission denial"
            )
        )
    }
}
