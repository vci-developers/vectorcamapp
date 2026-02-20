package com.vci.vectorcamapp.core.logging

/**
 * Context attached to Sentry breadcrumbs and exceptions for filtering and debugging.
 *
 * **Where to get IDs when building [CrashyContext]:**
 * - **programId**: [com.vci.vectorcamapp.core.domain.cache.DeviceCache.getProgramId] (Int → toString),
 *   or from screen state (e.g. [com.vci.vectorcamapp.landing.presentation.LandingState.enrolledProgram].id).
 * - **sessionId**: [com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache.getSession]?.localId?.toString()
 *   ([java.util.UUID] from [com.vci.vectorcamapp.core.domain.model.Session.localId]).
 * - **siteId**: [com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache.getSiteId] (Int → toString).
 * - **specimenId**: From imaging/specimen state (e.g. [com.vci.vectorcamapp.imaging.presentation.ImagingState.currentSpecimen].id).
 */
data class CrashyContext(
    val screen: String? = null,
    val feature: String? = null,
    val action: String? = null,
    val sessionId: String? = null,
    val programId: String? = null,
    val siteId: String? = null,
    val specimenId: String? = null
) {
    /**
     * Builds a [CrashyContext] from the given screen and optional IDs.
     * See class-level kdoc for where to get programId, sessionId, siteId, specimenId.
     */
    companion object {
        @JvmStatic
        fun fromIds(
            screen: String,
            feature: String? = null,
            action: String? = null,
            programId: String? = null,
            sessionId: String? = null,
            siteId: String? = null,
            specimenId: String? = null
        ) = CrashyContext(
            screen = screen,
            feature = feature,
            action = action,
            programId = programId,
            sessionId = sessionId,
            siteId = siteId,
            specimenId = specimenId
        )
    }
}
