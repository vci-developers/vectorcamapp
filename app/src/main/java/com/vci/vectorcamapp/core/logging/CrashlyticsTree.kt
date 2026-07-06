package com.vci.vectorcamapp.core.logging

import android.util.Log
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlytics
import timber.log.Timber

/**
 * Timber tree that forwards logs to Firebase Crashlytics.
 *
 * - VERBOSE / DEBUG: skipped (too noisy for crash reports)
 * - INFO / WARN: forwarded as breadcrumb logs (visible in the Logs tab of a crash report)
 * - ERROR: breadcrumb + records the throwable so it appears as a non-fatal in Crashlytics
 */
class CrashlyticsTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean =
        priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val crashlytics = VectorCamCrashlytics.crashlytics ?: return

        val label = when (priority) {
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "WTF"
            else -> "?"
        }
        val entry = if (tag != null) "[$label/$tag] $message" else "[$label] $message"
        crashlytics.log(entry)

        if (priority == Log.ERROR && t != null) {
            crashlytics.recordException(t)
        }
    }
}
