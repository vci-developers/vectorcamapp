package com.vci.vectorcamapp.core.logging

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.vci.vectorcamapp.core.domain.model.Device
import java.io.File

/**
 * App-wide analytics wrapper around FirebaseAnalytics.
 *
 * Screen time tracking works in two complementary ways:
 *  1. Standard GA4 — each [screenView] call fires a SCREEN_VIEW event. GA4 automatically
 *     associates subsequent `user_engagement` events with the active screen, giving you
 *     "Avg. engagement time per screen" in the Engagement > Pages and screens report.
 *  2. Explicit — before switching screens, a custom `screen_time` event is fired with the
 *     exact duration in milliseconds, visible as a custom event in GA4 Explorer.
 *
 * Device condition (battery + temperature) is tracked as user properties so every event
 * in GA4 carries the device state at the time it was fired.
 */
object VectorAnalytics {

    private const val TAG = "VectorAnalytics"

    @Volatile
    var analytics: FirebaseAnalytics? = null

    /** Application context — set once from VectorCamApp.onCreate(). */
    @Volatile
    var appContext: Context? = null

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

    // ── Device condition snapshot ─────────────────────────────────────────────

    data class DeviceCondition(
        /** 0–100 % */
        val batteryLevelPct: Int,
        /** Battery temperature in °C. Normal < 40°C. Overheating > 45°C. */
        val batteryTempC: Float,
        /** CPU/board temperature in °C read from thermal zone 0. Null when unavailable. */
        val cpuTempC: Float?,
        val isCharging: Boolean,
    )

    @Volatile
    private var lastCondition: DeviceCondition? = null

    /**
     * Reads the current battery level, battery temperature, CPU temperature, and charging
     * state from the system and stores them as Firebase user properties so they are
     * automatically attached to every subsequent event.
     *
     * Call this at app start and before key events (session start, upload, imaging).
     */
    fun updateDeviceCondition() {
        val condition = readDeviceCondition() ?: return
        lastCondition = condition

        if (debugLogging) {
            Log.d(TAG, buildString {
                append("DEVICE_CONDITION →")
                append(" battery=${condition.batteryLevelPct}%")
                append(" batt_temp=${condition.batteryTempC}°C")
                condition.cpuTempC?.let { append(" cpu_temp=${it}°C") }
                append(" charging=${condition.isCharging}")
            })
        }

        if (!enabled) return

        // Firebase user property name limit: 24 chars. Value limit: 36 chars.
        analytics?.setUserProperty("battery_level", condition.batteryLevelPct.toString())
        analytics?.setUserProperty("battery_temp_c", "%.1f".format(condition.batteryTempC))
        analytics?.setUserProperty("is_charging", condition.isCharging.toString())
        condition.cpuTempC?.let {
            analytics?.setUserProperty("cpu_temp_c", "%.1f".format(it))
        }
    }

    private fun readDeviceCondition(): DeviceCondition? {
        val ctx = appContext ?: return null
        val intent = ctx.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN
        )

        val batteryPct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val batteryTempC = tempRaw / 10.0f
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return DeviceCondition(
            batteryLevelPct = batteryPct,
            batteryTempC = batteryTempC,
            cpuTempC = readCpuTemperatureC(),
            isCharging = isCharging,
        )
    }

    /**
     * Reads CPU temperature from the thermal subsystem (thermal_zone0).
     * Available on most Android devices; returns null if the file is missing or unreadable.
     * The file stores the value in millidegrees Celsius on most SoCs.
     */
    private fun readCpuTemperatureC(): Float? = try {
        val raw = File("/sys/class/thermal/thermal_zone0/temp").readText().trim().toLong()
        // Values > 1000 are in millidegrees; ≤ 1000 are already in degrees
        if (raw > 1_000) raw / 1_000.0f else raw.toFloat()
    } catch (_: Exception) {
        null
    }

    /** Returns the last-read condition as a flat map suitable for event parameters. */
    private fun conditionParams(): Map<String, Any?> {
        val c = lastCondition ?: return emptyMap()
        return buildMap {
            put("battery_level", c.batteryLevelPct)
            put("battery_temp_c", "%.1f".format(c.batteryTempC))
            put("is_charging", c.isCharging.toString())
            c.cpuTempC?.let { put("cpu_temp_c", "%.1f".format(it)) }
        }
    }

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

        val condition = conditionParams()

        if (debugLogging) {
            val condStr = if (condition.isEmpty()) "" else " | ${condition.entries.joinToString { "${it.key}=${it.value}" }}"
            Log.d(TAG, "SCREEN_VIEW → $screenName$condStr")
        }

        if (!enabled) return

        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            condition.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putLong(key, value.toLong())
                    is Long -> putLong(key, value)
                    else -> putString(key, value.toString())
                }
            }
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
    // Device condition is automatically included in events where thermal state matters.

    fun sessionStarted(sessionId: String, collectionMethod: String) {
        updateDeviceCondition()
        logEvent(
            name = "session_started",
            params = mapOf(
                "session_id" to sessionId,
                "collection_method" to collectionMethod,
            ) + conditionParams()
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
        updateDeviceCondition()
        logEvent(
            name = "specimen_captured",
            params = mapOf(
                "session_id" to sessionId,
                "session_unit_id" to (sessionUnitId ?: "none"),
            ) + conditionParams()
        )
    }

    fun uploadStarted(sessionId: String) {
        updateDeviceCondition()
        logEvent(
            name = "upload_started",
            params = mapOf("session_id" to sessionId) + conditionParams()
        )
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
        updateDeviceCondition()
        logEvent(
            name = "upload_failed",
            params = mapOf(
                "session_id" to sessionId,
                "reason" to reason,
            ) + conditionParams()
        )
    }
}
