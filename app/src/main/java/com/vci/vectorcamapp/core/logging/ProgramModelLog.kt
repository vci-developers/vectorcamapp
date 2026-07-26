package com.vci.vectorcamapp.core.logging

import timber.log.Timber

/**
 * All program-model download logs use the fixed tag [TAG] so they can be filtered in Logcat with:
 * `adb logcat -s ProgramModel`
 */
object ProgramModelLog {
    const val TAG = "ProgramModel"

    fun v(message: String, vararg args: Any?) {
        Timber.tag(TAG).v(message, *args)
    }

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
