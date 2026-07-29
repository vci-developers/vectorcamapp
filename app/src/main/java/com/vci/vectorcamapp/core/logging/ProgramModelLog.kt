package com.vci.vectorcamapp.core.logging

import android.util.Log
import timber.log.Timber

/**
 * All program-model download logs use the fixed tag [TAG] so they can be filtered in Logcat with:
 * `adb logcat -s ProgramModel`
 *
 * Writes directly to [Log] (in addition to Timber) so the tag is reliably visible in Logcat
 * regardless of which Timber tree is planted or when.
 */
object ProgramModelLog {
    const val TAG = "ProgramModel"

    fun d(message: String, vararg args: Any?) {
        val formatted = format(message, *args)
        Log.d(TAG, formatted)
        Timber.tag(TAG).d(formatted)
    }

    fun i(message: String, vararg args: Any?) {
        val formatted = format(message, *args)
        Log.i(TAG, formatted)
        Timber.tag(TAG).i(formatted)
    }

    fun w(message: String, vararg args: Any?) {
        val formatted = format(message, *args)
        Log.w(TAG, formatted)
        Timber.tag(TAG).w(formatted)
    }

    fun e(message: String, vararg args: Any?) {
        val formatted = format(message, *args)
        Log.e(TAG, formatted)
        Timber.tag(TAG).e(formatted)
    }

    fun e(throwable: Throwable, message: String, vararg args: Any?) {
        val formatted = format(message, *args)
        Log.e(TAG, formatted, throwable)
        Timber.tag(TAG).e(throwable, formatted)
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

    private fun format(message: String, vararg args: Any?): String {
        if (args.isEmpty()) return message
        return try {
            String.format(message, *args)
        } catch (_: Exception) {
            "$message | args=${args.joinToString()}"
        }
    }
}
