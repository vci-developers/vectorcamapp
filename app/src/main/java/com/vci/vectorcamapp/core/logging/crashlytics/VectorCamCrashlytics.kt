package com.vci.vectorcamapp.core.logging.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vci.vectorcamapp.core.domain.model.Device

object VectorCamCrashlytics {

    @Volatile var enabled: Boolean = true
    @Volatile var crashlytics: FirebaseCrashlytics? = null

    // ---- Breadcrumbs / diagnostic logs -------------------------------------

    fun log(
        message: String,
        category: String = "app",
        severity: Severity = Severity.INFO,
        data: Map<String, Any?> = emptyMap(),
        context: CrashyContext? = null,
    ) {
        if (!enabled) return
        val crashlyticsInstance = crashlytics ?: return
        val parts = buildList {
            add("[${severity.name}][$category] $message")
            context?.screen?.let     { add("screen=$it") }
            context?.feature?.let    { add("feature=$it") }
            context?.action?.let     { add("action=$it") }
            context?.sessionId?.let  { add("session_id=$it") }
            context?.programId?.let  { add("program_id=$it") }
            context?.siteId?.let     { add("site_id=$it") }
            context?.specimenId?.let { add("specimen_id=$it") }
            data.forEach { (key, value) -> add("$key=$value") }
        }
        crashlyticsInstance.log(parts.joinToString(" | ").take(1024))
    }

    // ---- Non-fatal exceptions ----------------------------------------------

    fun exception(
        throwable: Throwable,
        severity: Severity = Severity.ERROR,
        context: CrashyContext? = null,
        tags: Map<String, String> = emptyMap(),
        extras: Map<String, Any?> = emptyMap(),
    ) {
        if (!enabled) return
        val crashlyticsInstance = crashlytics ?: return

        // Standard 7 — always set/refresh on every exception call
        crashlyticsInstance.setCustomKey("severity",    severity.name)
        crashlyticsInstance.setCustomKey("screen",      context?.screen.orEmpty())
        crashlyticsInstance.setCustomKey("feature",     context?.feature.orEmpty())
        crashlyticsInstance.setCustomKey("action",      context?.action.orEmpty())
        crashlyticsInstance.setCustomKey("session_id",  context?.sessionId.orEmpty())
        crashlyticsInstance.setCustomKey("program_id",  context?.programId.orEmpty())
        crashlyticsInstance.setCustomKey("site_id",     context?.siteId.orEmpty())
        crashlyticsInstance.setCustomKey("specimen_id", context?.specimenId.orEmpty())

        // Caller-supplied
        tags.forEach   { (key, value) -> crashlyticsInstance.setCustomKey(key.take(64), value.take(1024)) }
        extras.forEach { (key, value) -> crashlyticsInstance.setCustomKey(key.take(64), value.toString().take(1024)) }

        crashlyticsInstance.recordException(throwable)
    }

    // ---- User identity ------------------------------------------------------

    fun setDevice(device: Device?) {
        if (!enabled) return
        val crashlyticsInstance = crashlytics ?: return
        if (device == null) {
            crashlyticsInstance.setUserId("")
            return
        }
        val userId = "${device.id}_${device.registeredAt}"
        crashlyticsInstance.setUserId(userId)
        crashlyticsInstance.setCustomKey("device_model", device.model)
        log(
            message = "User context set",
            category = "user",
            severity = Severity.INFO,
            data = mapOf("user_id" to userId, "username" to device.model),
        )
    }

    fun clearDevice() {
        if (!enabled) return
        val crashlyticsInstance = crashlytics ?: return
        crashlyticsInstance.setUserId("")
        log("Device context cleared", category = "user")
    }
}
