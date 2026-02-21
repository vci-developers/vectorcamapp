package com.vci.vectorcamapp.complete_session.list.presentation

import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.helpers.SessionUploadProgress

data class CompleteSessionListState(
    val sessionAndSiteToUploadProgress: Map<SessionAndSite, SessionUploadProgress> = emptyMap(),
    val searchQuery: String = "",
    val isSearchTooltipVisible: Boolean = false,
    /** Current session from cache (if any) for CrashyContext. */
    val currentSessionId: String? = null,
    /** Current site id from cache (if any) for CrashyContext. */
    val currentSiteId: String? = null
)
