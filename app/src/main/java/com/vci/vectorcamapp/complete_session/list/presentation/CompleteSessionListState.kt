package com.vci.vectorcamapp.complete_session.list.presentation

import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import java.util.UUID

data class CompleteSessionListState(
    val sessionsAndSites: List<SessionAndSite> = emptyList(),
    val isUploading: Boolean = false,
    val specimensBySession: Map<UUID, List<SpecimenWithSpecimenImagesAndInferenceResults>> = emptyMap()
)
