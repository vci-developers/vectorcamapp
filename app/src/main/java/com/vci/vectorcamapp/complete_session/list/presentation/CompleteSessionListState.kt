package com.vci.vectorcamapp.complete_session.list.presentation

import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.helpers.SessionUploadProgress

data class CompleteSessionListState(
    val sessionAndSiteToUploadProgress: Map<SessionAndSite, SessionUploadProgress> = emptyMap(),
    val searchQuery: String = "",
    val isSearchTooltipVisible: Boolean = false
)
