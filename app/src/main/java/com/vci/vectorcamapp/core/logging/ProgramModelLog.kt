package com.vci.vectorcamapp.core.logging

import timber.log.Timber

/**
 * All program-model download logs use the fixed tag [TAG] so they can be filtered in Logcat with:
 * `adb logcat -s ProgramModel`
 *
 * Uses Timber only (not [android.util.Log] directly) — Timber.DebugTree already forwards to
 * Logcat in debug builds, and calling android.util.Log directly crashes plain JVM unit tests
 * (Robolectric-less) with "Method ... not mocked".
 */
object ProgramModelLog {
    const val TAG = "ProgramModel"

    fun d(message: String, vararg args: Any?) {
        Timber.tag(TAG).d(message, *args)
    }

    fun i(message: String, vararg args: Any?) {
        Timber.tag(TAG).i(message, *args)
    }

    fun w(message: String, vararg args: Any?) {
        Timber.tag(TAG).w(message, *args)
    }

    fun e(message: String, vararg args: Any?) {
        Timber.tag(TAG).e(message, *args)
    }

    fun e(throwable: Throwable, message: String, vararg args: Any?) {
        Timber.tag(TAG).e(throwable, message, *args)
    }

    /** Redacts query string from presigned URLs (contains credentials). */
    fun redactUrl(url: String): String {
        val queryIndex = url.indexOf('?')
        return if (queryIndex >= 0) {
            url.substring(0, queryIndex) + "?[REDACTED]"
        } else {
            url
        }
    }
}
