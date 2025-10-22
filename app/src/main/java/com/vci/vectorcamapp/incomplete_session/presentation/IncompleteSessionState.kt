package com.vci.vectorcamapp.incomplete_session.presentation

import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import java.util.UUID

data class IncompleteSessionState(
    val sessionAndSites: List<SessionAndSite> = emptyList(),
    val deleteDialogSessionId: UUID? = null,
    val searchQuery: String = ""
)
