package com.vci.vectorcamapp.core.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vci.vectorcamapp.core.domain.model.Device

object Crashy {

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
        val cl = crashlytics ?: return
        val parts = buildList {
            add("[${severity.name}][$category] $message")
            context?.screen?.let     { add("screen=$it") }
            context?.feature?.let    { add("feature=$it") }
            context?.action?.let     { add("action=$it") }
            context?.sessionId?.let  { add("session_id=$it") }
            context?.programId?.let  { add("program_id=$it") }
            context?.siteId?.let     { add("site_id=$it") }
            context?.specimenId?.let { add("specimen_id=$it") }
            data.forEach { (k, v) -> add("$k=$v") }
        }
        cl.log(parts.joinToString(" | ").take(1024))
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
        val cl = crashlytics ?: return

        // Standard 7 — always set/refresh on every exception call
        cl.setCustomKey("severity", severity.name)
        cl.setCustomKey("screen",      context?.screen.orEmpty())
        cl.setCustomKey("feature",     context?.feature.orEmpty())
        cl.setCustomKey("action",      context?.action.orEmpty())
        cl.setCustomKey("session_id",  context?.sessionId.orEmpty())
        cl.setCustomKey("program_id",  context?.programId.orEmpty())
        cl.setCustomKey("site_id",     context?.siteId.orEmpty())
        cl.setCustomKey("specimen_id", context?.specimenId.orEmpty())

        // Caller-supplied
        tags.forEach   { (k, v) -> cl.setCustomKey(k.take(64), v.take(1024)) }
        extras.forEach { (k, v) -> cl.setCustomKey(k.take(64), v.toString().take(1024)) }

        cl.recordException(throwable)
    }

    // ---- User identity ------------------------------------------------------

    fun setDevice(device: Device?) {
        if (!enabled) return
        val cl = crashlytics ?: return
        if (device == null) {
            cl.setUserId("")
            return
        }
        val userId = "${device.id}_${device.registeredAt}"
        cl.setUserId(userId)
        cl.setCustomKey("device_model", device.model)
        log(
            message = "User context set",
            category = "user",
            severity = Severity.INFO,
            data = mapOf("user_id" to userId, "username" to device.model),
        )
    }

    fun clearDevice() {
        if (!enabled) return
        val cl = crashlytics ?: return
        cl.setUserId("")
        log("Device context cleared", category = "user")
    }
}
