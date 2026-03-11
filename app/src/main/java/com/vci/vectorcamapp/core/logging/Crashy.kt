package com.vci.vectorcamapp.core.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vci.vectorcamapp.core.domain.model.Device
import io.sentry.Attachment
import io.sentry.Breadcrumb
import io.sentry.IScope
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.protocol.SentryId
import io.sentry.protocol.User

object Crashy {

    @Volatile
    var enabled = true

    fun globalBreadcrumb(
        message: String,
        category: String = "app",
        level: SentryLevel = SentryLevel.INFO,
        data: Map<String, Any?> = emptyMap(),
        context: CrashyContext? = null
    ) {
        if (!enabled) return
        val breadcrumb = Breadcrumb().apply {
            this.message = message
            this.category = category
            this.level = level
            data.forEach { (key, value) -> setData(key, value.toString()) }
            context?.let { applyContextToBreadcrumb(this, it) }
        }
        Sentry.addBreadcrumb(breadcrumb)
        logBreadcrumbToFirebase(message, category, level, data, context)
    }

    fun scopedBreadcrumb(
        message: String,
        category: String = "app",
        level: SentryLevel = SentryLevel.INFO,
        data: Map<String, Any?> = emptyMap(),
        context: CrashyContext? = null
    ) {
        if (!enabled) return
        Sentry.pushScope().use {
            Sentry.configureScope { scope ->
                val breadcrumb = Breadcrumb().apply {
                    this.message = message
                    this.category = category
                    this.level = level
                    data.forEach { (key, value) -> setData(key, value.toString()) }
                    context?.let { applyContextToBreadcrumb(this, it) }
                }
                scope.addBreadcrumb(breadcrumb)
            }
        }
        logBreadcrumbToFirebase(message, category, level, data, context)
    }

    fun exception(
        throwable: Throwable,
        level: SentryLevel = SentryLevel.ERROR,
        context: CrashyContext? = null,
        tags: Map<String, String> = emptyMap(),
        extras: Map<String, Any?> = emptyMap(),
        attachments: List<Attachment> = emptyList()
    ): SentryId? {
        if (!enabled) return null
        var id: SentryId? = null
        Sentry.pushScope().use {
            Sentry.configureScope { scope ->
                scope.level = level
                context?.let { applyContextToScope(scope, it) }
                tags.forEach(scope::setTag)
                extras.forEach { (key, value) -> scope.setExtra(key, value.toString()) }
                attachments.forEach(scope::addAttachment)
                id = Sentry.captureException(throwable)
            }
        }
        val crashlytics = FirebaseCrashlytics.getInstance()
        context?.let { ctx ->
            ctx.screen?.let { crashlytics.setCustomKey("screen", it) }
            ctx.feature?.let { crashlytics.setCustomKey("feature", it) }
            ctx.action?.let { crashlytics.setCustomKey("action", it) }
            ctx.sessionId?.let { crashlytics.setCustomKey("session_id", it) }
            ctx.programId?.let { crashlytics.setCustomKey("program_id", it) }
            ctx.siteId?.let { crashlytics.setCustomKey("site_id", it) }
            ctx.specimenId?.let { crashlytics.setCustomKey("specimen_id", it) }
        }
        tags.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
        crashlytics.recordException(throwable)
        return id
    }

    fun setDevice(device: Device?) {
        if (!enabled) return
        val crashlytics = FirebaseCrashlytics.getInstance()
        if (device == null) {
            Sentry.setUser(null)
            crashlytics.setUserId("")
            return
        }
        val userId = "${device.id}_${device.registeredAt}"
        val user = User().apply {
            this.id = userId
            this.username = device.model
        }
        Sentry.setUser(user)
        crashlytics.setUserId(userId)
        crashlytics.setCustomKey("device_model", device.model)
        globalBreadcrumb(
            message = "User context set",
            category = "user",
            level = SentryLevel.INFO,
            data = mapOf(
                "user_id" to userId,
                "username" to device.model,
            )
        )
    }

    fun clearDevice() {
        if (!enabled) return
        Sentry.setUser(null)
        FirebaseCrashlytics.getInstance().setUserId("")
        globalBreadcrumb(
            message = "Device context cleared",
            category = "user",
            level = SentryLevel.INFO
        )
    }

    private fun logBreadcrumbToFirebase(
        message: String,
        category: String,
        level: SentryLevel,
        data: Map<String, Any?>,
        context: CrashyContext?
    ) {
        val parts = buildList {
            add("[${level.name}][$category] $message")
            context?.screen?.let { add("screen=$it") }
            context?.feature?.let { add("feature=$it") }
            context?.action?.let { add("action=$it") }
            context?.sessionId?.let { add("session_id=$it") }
            context?.programId?.let { add("program_id=$it") }
            context?.siteId?.let { add("site_id=$it") }
            context?.specimenId?.let { add("specimen_id=$it") }
            data.forEach { (key, value) -> add("$key=$value") }
        }
        FirebaseCrashlytics.getInstance().log(parts.joinToString(" | "))
    }

    private fun applyContextToScope(scope: IScope, context: CrashyContext) {
        context.screen?.let { scope.setTag("screen", it) }
        context.feature?.let { scope.setTag("feature", it) }
        context.action?.let { scope.setTag("action", it) }
        context.sessionId?.let { scope.setTag("session_id", it) }
        context.programId?.let { scope.setTag("program_id", it) }
        context.siteId?.let { scope.setTag("site_id", it) }
        context.specimenId?.let { scope.setTag("specimen_id", it) }
    }

    private fun applyContextToBreadcrumb(breadcrumb: Breadcrumb, context: CrashyContext) {
        context.screen?.let { breadcrumb.setData("screen", it) }
        context.feature?.let { breadcrumb.setData("feature", it) }
        context.action?.let { breadcrumb.setData("action", it) }
        context.sessionId?.let { breadcrumb.setData("session_id", it) }
        context.programId?.let { breadcrumb.setData("program_id", it) }
        context.siteId?.let { breadcrumb.setData("site_id", it) }
        context.specimenId?.let { breadcrumb.setData("specimen_id", it) }
    }
}
