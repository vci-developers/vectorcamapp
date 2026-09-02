package com.vci.vectorcamapp.core.logging.analytics

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber
import com.vci.vectorcamapp.BuildConfig
import com.vci.vectorcamapp.core.domain.model.Device
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import com.vci.vectorcamapp.core.logging.crashlytics.VectorCamCrashlytics

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
 * in GA4 carries the device state at the time it was fired. Flavor [BuildConfig.APPLICATION_ID]
 * is sent as `app_package` on every event and as a user property.
 */
object VectorCamAnalytics {

    @Volatile
    var analytics: FirebaseAnalytics? = null

    /** Application context — set once from VectorCamApp.onCreate(). */
    @Volatile
    var appContext: Context? = null

    /** Set to false to suppress sending events to Firebase (e.g. in debug builds). */
    @Volatile
    var enabled = true

    // ── Screen time bookkeeping ───────────────────────────────────────────────

    private var currentScreen: String? = null
    private var screenEnteredAt: Long = 0L

    // ── Device condition snapshot ─────────────────────────────────────────────

    data class DeviceCondition(
        val batteryLevelPct: Int,
        val batteryTempC: Float,
        val cpuTempC: Float?,
        val isCharging: Boolean,
        val networkType: String,
        val availableStorageMb: Long,
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

        Timber.d(buildString {
            append("DEVICE_CONDITION →")
            append(" battery=${condition.batteryLevelPct}%")
            append(" batt_temp=${condition.batteryTempC}°C")
            condition.cpuTempC?.let { append(" cpu_temp=${it}°C") }
            append(" charging=${condition.isCharging}")
            append(" network=${condition.networkType}")
            append(" storage=${condition.availableStorageMb}MB")
        })

        if (!enabled) return

        analytics?.setUserProperty("battery_level", condition.batteryLevelPct.toString())
        analytics?.setUserProperty("battery_temp_c", "%.1f".format(condition.batteryTempC))
        analytics?.setUserProperty("is_charging", condition.isCharging.toString())
        condition.cpuTempC?.let {
            analytics?.setUserProperty("cpu_temp_c", "%.1f".format(it))
        }
        analytics?.setUserProperty("network_type", condition.networkType)
        analytics?.setUserProperty("storage_available_mb", condition.availableStorageMb.toString())
    }

    /**
     * Sets static device properties that don't change at runtime.
     * Call once from VectorCamApp.onCreate() after [analytics] is assigned.
     */
    fun setStaticProperties() {
        Timber.d(
            "STATIC_PROPS → package=${BuildConfig.APPLICATION_ID} " +
                "app=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) " +
                "android=${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        )
        if (!enabled) return
        analytics?.setUserProperty("app_package", BuildConfig.APPLICATION_ID)
        analytics?.setUserProperty("app_version", BuildConfig.VERSION_NAME)
        analytics?.setUserProperty("app_build", BuildConfig.VERSION_CODE.toString())
        analytics?.setUserProperty("android_version", Build.VERSION.RELEASE)
        analytics?.setUserProperty("android_sdk", Build.VERSION.SDK_INT.toString())
    }

    /** Params attached to every tracked event (flavor applicationId). */
    private fun commonParams(): Map<String, Any?> =
        mapOf("app_package" to BuildConfig.APPLICATION_ID)

    private fun readDeviceCondition(): DeviceCondition? {
        val context = appContext ?: return null
        val intent = context.registerReceiver(
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
            networkType = readNetworkType(context),
            availableStorageMb = readAvailableStorageMb(),
        )
    }

    private fun readNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
    }

    private fun readAvailableStorageMb(): Long = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024)
    } catch (_: Exception) {
        -1L
    }

    private fun readCpuTemperatureC(): Float? = try {
        val raw = File("/sys/class/thermal/thermal_zone0/temp").readText().trim().toLong()
        if (raw > 1_000) raw / 1_000.0f else raw.toFloat()
    } catch (_: Exception) {
        null
    }

    private fun conditionParams(): Map<String, Any?> {
        val condition = lastCondition ?: return emptyMap()
        return buildMap {
            // Numeric event params — register as GA4 Custom metrics for > / < filters.
            // User properties above must stay strings (Firebase API limitation).
            put("battery_level", condition.batteryLevelPct)
            put("battery_temp_c", condition.batteryTempC.toDouble())
            put("is_charging", condition.isCharging.toString())
            condition.cpuTempC?.let { put("cpu_temp_c", it.toDouble()) }
            put("network_type", condition.networkType)
            put("storage_available_mb", condition.availableStorageMb)
        }
    }

    private fun putParam(bundle: Bundle, key: String, value: Any?) {
        when (value) {
            null -> {}
            is String -> bundle.putString(key, value)
            is Long -> bundle.putLong(key, value)
            is Int -> bundle.putLong(key, value.toLong())
            is Double -> bundle.putDouble(key, value)
            is Float -> bundle.putDouble(key, value.toDouble())
            is Boolean -> bundle.putString(key, value.toString())
            else -> bundle.putString(key, value.toString())
        }
    }

    // ── Screen tracking ───────────────────────────────────────────────────────

    /**
     * Call once per navigation destination change.
     *
     * Logs a [FirebaseAnalytics.Event.SCREEN_VIEW] for the new screen and, if a previous
     * screen was active, immediately logs a `screen_time` event with how long the user
     * spent on it (in ms and seconds for convenience).
     *
     * Also appends a Crashlytics breadcrumb so crash reports carry a navigation trail.
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

        // Crashlytics breadcrumb — navigation trail in crash reports
        VectorCamCrashlytics.log(
            message = "screen_view",
            category = "nav",
            data = mapOf("screen" to screenName, "class" to screenClass),
        )

        val eventParams = commonParams() + conditionParams()

        val paramsString = if (eventParams.isEmpty()) "" else " | ${eventParams.entries.joinToString { "${it.key}=${it.value}" }}"
        Timber.d("SCREEN_VIEW → $screenName$paramsString")

        if (!enabled) return
        // Bail out before touching Bundle when no Firebase instance is attached
        // (also keeps plain-JVM unit tests off unmocked android.os.Bundle APIs).
        val firebaseAnalytics = analytics ?: return

        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            eventParams.forEach { (key, value) -> putParam(this, key, value) }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    // ── Generic event logging ─────────────────────────────────────────────────

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        val mergedParams = commonParams() + conditionParams() + params
        val paramsStr = if (mergedParams.isEmpty()) "" else " | ${mergedParams.entries.joinToString { "${it.key}=${it.value}" }}"
        Timber.d("EVENT → $name$paramsStr")
        if (!enabled) return
        // Bail out before touching Bundle when no Firebase instance is attached
        // (also keeps plain-JVM unit tests off unmocked android.os.Bundle APIs).
        val firebaseAnalytics = analytics ?: return
        val bundle = Bundle()
        mergedParams.forEach { (key, value) -> putParam(bundle, key, value) }
        firebaseAnalytics.logEvent(name, bundle)
    }

    // ── User / device identity ────────────────────────────────────────────────

    /**
     * Sets the GA4 user id and all cohort-defining user properties required for retention analysis.
     *
     * Call after successful registration and on every cold start (re-hydration from cache).
     * [programId] is stored separately from [Device] in [DeviceCache]; pass it alongside the device.
     */
    fun setDevice(device: Device?, programId: Int? = null) {
        if (!enabled) return
        if (device == null) {
            analytics?.setUserId(null)
            listOf(
                "device_model", "registration_cohort_week", "registered_at_date",
                "program_id", "site_id"
            ).forEach { analytics?.setUserProperty(it, null) }
            return
        }
        val userId = "${device.id}_${device.registeredAt}"
        analytics?.setUserId(userId)
        analytics?.setUserProperty("device_model", device.model)

        // Cohort dimensions for GA4 retention analysis
        val date = Instant.ofEpochMilli(device.registeredAt)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val weekFields = WeekFields.ISO
        val cohortWeek = "%d-W%02d".format(
            date.get(weekFields.weekBasedYear()),
            date.get(weekFields.weekOfWeekBasedYear()),
        )
        analytics?.setUserProperty("registration_cohort_week", cohortWeek)
        analytics?.setUserProperty("registered_at_date", date.toString())
        programId?.let { analytics?.setUserProperty("program_id", it.toString()) }

        Timber.d("SET_DEVICE → user_id=$userId cohort_week=$cohortWeek program_id=$programId")
    }

    /**
     * Updates the `site_id` user property. Call after a successful intake form submission so
     * GA4 cohort retention reports can be faceted by site.
     */
    fun setSiteContext(siteId: Int) {
        if (!enabled) return
        analytics?.setUserProperty("site_id", siteId.toString())
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
