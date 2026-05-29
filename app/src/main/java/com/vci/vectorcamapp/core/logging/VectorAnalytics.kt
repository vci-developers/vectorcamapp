package com.vci.vectorcamapp.core.logging

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.vci.vectorcamapp.core.domain.model.Device

/**
 * App-wide analytics wrapper around FirebaseAnalytics.
 *
 * Screen time tracking works in two complementary ways:
 *  1. Standard GA4 — each [screenView] call fires a SCREEN_VIEW event. GA4 automatically
 *     associates subsequent `user_engagement` events with the active screen, giving you
 *     "Avg. engagement time per screen" in the Engagement > Pages and screens report.
 *  2. Explicit — before switching screens, a custom `screen_time` event is fired with the
 *     exact duration in milliseconds, visible as a custom event in GA4 Explorer.
 */
object VectorAnalytics {

    private const val TAG = "VectorAnalytics"

    @Volatile
    var analytics: FirebaseAnalytics? = null

    /** Set to false to suppress sending events to Firebase (e.g. in debug builds). */
    @Volatile
    var enabled = true

    /**
     * When true, every event and screen view is printed to Logcat regardless of [enabled].
     * Enable this in debug builds to verify tracking without sending real data.
     */
    @Volatile
    var debugLogging = false

    // ── Screen time bookkeeping ───────────────────────────────────────────────

    private var currentScreen: String? = null
    private var screenEnteredAt: Long = 0L

    // ── Screen tracking ───────────────────────────────────────────────────────

    /**
     * Call once per navigation destination change.
     *
     * Logs a [FirebaseAnalytics.Event.SCREEN_VIEW] for the new screen and, if a previous
     * screen was active, immediately logs a `screen_time` event with how long the user
     * spent on it (in ms and seconds for convenience).
     */
    fun screenView(screenName: String, screenClass: String = screenName) {
        val previous = currentScreen
        val enteredAt = screenEnteredAt
        if (previous != null && enteredAt > 0L) {
            val durationMs = System.currentTimeMillis() - enteredAt
            logEvent(
                name = "screen_time",
                params = mapOf(
                    "screen_name" to previous,
                    "duration_ms" to durationMs,
                    "duration_sec" to durationMs / 1000L,
                )
            )
        }

        currentScreen = screenName
        screenEnteredAt = System.currentTimeMillis()

        if (debugLogging) {
            Log.d(TAG, "SCREEN_VIEW → $screenName")
        }

        if (!enabled) return

        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    // ── Generic event logging ─────────────────────────────────────────────────

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        if (debugLogging) {
            val paramsStr = if (params.isEmpty()) "" else " | ${params.entries.joinToString { "${it.key}=${it.value}" }}"
            Log.d(TAG, "EVENT → $name$paramsStr")
        }
        if (!enabled) return
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Long -> bundle.putLong(key, value)
                is Int -> bundle.putInt(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Boolean -> bundle.putString(key, value.toString())
                null -> {}
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics?.logEvent(name, bundle)
    }

    // ── User / device identity ────────────────────────────────────────────────

    fun setDevice(device: Device?) {
        if (!enabled) return
        if (device == null) {
            analytics?.setUserId(null)
            analytics?.setUserProperty("device_model", null)
            analytics?.setUserProperty("region", null)
            return
        }
        val userId = "${device.id}_${device.registeredAt}"
        analytics?.setUserId(userId)
        analytics?.setUserProperty("device_model", device.model)
    }

    fun setRegion(region: String) {
        if (!enabled) return
        analytics?.setUserProperty("region", region)
    }

    fun setUserProperty(name: String, value: String?) {
        if (!enabled) return
        analytics?.setUserProperty(name, value)
    }

    // ── Domain-specific helpers ───────────────────────────────────────────────

    fun sessionStarted(sessionId: String, collectionMethod: String) {
        logEvent(
            name = "session_started",
            params = mapOf(
                "session_id" to sessionId,
                "collection_method" to collectionMethod,
            )
        )
    }

    fun sessionCompleted(sessionId: String, specimenCount: Int) {
        logEvent(
            name = "session_completed",
            params = mapOf(
                "session_id" to sessionId,
                "specimen_count" to specimenCount,
            )
        )
    }

    fun specimenCaptured(sessionId: String, sessionUnitId: String?) {
        logEvent(
            name = "specimen_captured",
            params = mapOf(
                "session_id" to sessionId,
                "session_unit_id" to (sessionUnitId ?: "none"),
            )
        )
    }

    fun uploadStarted(sessionId: String) {
        logEvent(name = "upload_started", params = mapOf("session_id" to sessionId))
    }

    fun uploadCompleted(sessionId: String, durationMs: Long) {
        logEvent(
            name = "upload_completed",
            params = mapOf(
                "session_id" to sessionId,
                "duration_ms" to durationMs,
            )
        )
    }

    fun uploadFailed(sessionId: String, reason: String) {
        logEvent(
            name = "upload_failed",
            params = mapOf(
                "session_id" to sessionId,
                "reason" to reason,
            )
        )
    }
}
