package com.vci.vectorcamapp.incomplete_session.logging

import android.net.Uri
import com.vci.vectorcamapp.core.logging.Crashy
import com.vci.vectorcamapp.core.logging.CrashyContext
import java.util.UUID

object IncompleteSessionSentryLogger {

    fun logSessionNotFound(e: Throwable, failedSessionId: UUID) {
        Crashy.exception(
            throwable = e,
                context = CrashyContext(
                screen = "InProgressSession",
                feature = "SessionResume",
                action = "session_not_found",
                sessionId = failedSessionId.toString()
            ),
                tags = mapOf(
                "error_type" to "session_not_found",
                    "user_impact" to "cannot_resume_session"
            ),
                extras = mapOf(
                "error_context" to "User cannot continue their work session",
                "requested_session_id" to failedSessionId,
                "recovery_action" to "User may need to start new session",
                "possible_causes" to "Session ID is outdated, session was deleted from database, mismatch between cache and DB, corrupted local storage"
            )
        )
    }

    fun logSessionRetrievalFailure(e: Throwable, failedSessionId: UUID) {
        Crashy.exception(
            throwable = e,
                context = CrashyContext(
                screen = "InProgressSession",
                feature = "SessionResume",
                action = "session_retrieval",
                sessionId = failedSessionId.toString()
            ),
                tags = mapOf(
                "error_type" to "session_retrieval_failed",
                "critical" to "true",
                "user_blocking" to "true"
            ),
                extras = mapOf(
                "error_context" to "User cannot resume their in progress session",
                "requested_session_id" to failedSessionId,
                "recovery_action" to "User may need to start a new session",
                "data_loss_risk" to "Previous session work may be lost",
                "possible_causes" to "Room database query failure, coroutine cancellation, database migration issue, session ID mismatch, I/O error"
            )
        )
    }

    fun logSessionDeletionFailure(e: Throwable, failedSessionId: UUID) {
        Crashy.exception(
            throwable = e,
                context = CrashyContext(
                screen = "InProgressSession",
                feature = "InProgressSessionDeletion",
                action = "in_progress_session_deletion",
                sessionId = failedSessionId.toString()
            ),
                tags = mapOf(
                "error_type" to "in_progress_session_deletion_failed",
            ),
                extras = mapOf(
                "error_context" to "User cannot delete the in progress session",
                "requested_session_id" to failedSessionId,
                "recovery_action" to "User may need to try the delete action again",
                "possible_causes" to "Corrupted in progress session data"
            )
        )
    }

    fun logImageDeletionFailure(e: Throwable, failedSessionId: UUID, failedImageUri: Uri) {
        Crashy.exception(
            throwable = e,
                context = CrashyContext(
                screen = "InProgressSession",
                feature = "InProgressSessionImageDeletion",
                action = "in_progress_session_image_deletion",
                sessionId = failedSessionId.toString()
            ),
                tags = mapOf(
                "error_type" to "in_progress_session_image_deletion_failed",
            ),
                extras = mapOf(
                "error_context" to "User cannot delete the image from an in progress session",
                "requested_session_id" to failedSessionId,
                "requested_image_uri" to failedImageUri,
                "possible_causes" to "Corrupted in progress session data or image file"
            )
        )
    }
}
