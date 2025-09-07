package com.vci.vectorcamapp.complete_session.list.presentation

import com.vci.vectorcamapp.complete_session.list.domain.model.CompleteSessionListUploadCount
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import java.util.UUID

data class CompleteSessionListState(
    val sessionsAndSites: List<SessionAndSite> = emptyList(),
    val sessionUploadCounts: List<Pair<UUID, CompleteSessionListUploadCount>> = emptyList(),
    val activeUploadSessions: Set<UUID> = emptySet(),
    val isUploading: Boolean = false
)
