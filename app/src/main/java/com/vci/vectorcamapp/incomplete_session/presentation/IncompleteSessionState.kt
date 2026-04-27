package com.vci.vectorcamapp.incomplete_session.presentation

import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import java.util.UUID

data class IncompleteSessionState(
    val sessionAndSites: List<SessionAndSite> = emptyList(),
    val deleteDialogSessionId: UUID? = null,
    val searchQuery: String = "",
    val isSearchTooltipVisible: Boolean = false,
    /** Current session from cache (if any) for CrashyContext. */
    val currentSessionId: String? = null,
    /** Current site id from cache (if any) for CrashyContext. */
    val currentSiteId: String? = null
)
