package com.vci.vectorcamapp.complete_session.list.presentation

import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.model.helpers.SessionUploadProgress
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.search.SearchUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CompleteSessionListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val specimenRepository: SpecimenRepository,
    private val specimenImageRepository: SpecimenImageRepository,
    private val workManagerRepository: WorkManagerRepository
) : CoreViewModel() {

    private val _state = MutableStateFlow(CompleteSessionListState())

    private val _sessionAndSiteToUploadProgress = sessionRepository.observeCompleteSessionsAndSites()
        .flatMapLatest { sessions ->
            if (sessions.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(
                    sessions.map { sessionAndSite ->
                        val sessionId = sessionAndSite.session.localId
                        combine(
                            specimenImageRepository.observeUploadedImageCountForSession(sessionId),
                            workManagerRepository.observeIsSessionActivelyUploading(sessionId),
                        ) { uploadedCount, isUploading ->
                            sessionAndSite to SessionUploadProgress(
                                uploadedImageCount = uploadedCount,
                                totalImageCount = specimenImageRepository.getTotalImageCountForSession(
                                    sessionId
                                ),
                                isUploading = isUploading
                            )
                        }
                    }
                ) { progress ->
                    progress.toMap()
                }
            }
        }

    val state = combine(
        _sessionAndSiteToUploadProgress,
        _state
    ) { sessionAndSiteToUploadProgress, currentState ->
        val filteredProgressMap = if (currentState.searchQuery.isBlank()) {
            sessionAndSiteToUploadProgress
        } else {
            sessionAndSiteToUploadProgress.filter { (sessionAndSite, _) ->
                val session = sessionAndSite.session
                val site = sessionAndSite.site
                val fieldsForSearch = buildList {
                    add(session.collectorName)
                    add(session.collectorTitle)
                    add(session.collectionMethod)
                    add(session.specimenCondition)
                    add(session.type.name)
                    add(site.district)
                    add(site.subCounty)
                    add(site.parish)
                    add(site.villageName)
                    add(site.houseNumber)
                    add(site.healthCenter)
                }
                SearchUtils.matchesQuery(currentState.searchQuery, fieldsForSearch)
            }
        }
        currentState.copy(sessionAndSiteToUploadProgress = filteredProgressMap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CompleteSessionListState())

    private val _events = Channel<CompleteSessionListEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: CompleteSessionListAction) {
        viewModelScope.launch {
            when (action) {
                CompleteSessionListAction.ReturnToLandingScreen -> {
                    _events.send(CompleteSessionListEvent.NavigateBackToLandingScreen)
                }

                is CompleteSessionListAction.ViewCompleteSessionDetails -> {
                    _events.send(CompleteSessionListEvent.NavigateToCompleteSessionDetails(action.sessionId))
                }

                is CompleteSessionListAction.UploadAllPendingSessions -> {
                    val completeSessionsAndSites =
                        sessionRepository.observeCompleteSessionsAndSites().first()
                    for (sessionAndSite in completeSessionsAndSites) {
                        val session = sessionAndSite.session
                        val site = sessionAndSite.site

                        val isSessionUploaded = (session.submittedAt != null)

                        val specimens =
                            specimenRepository.getSpecimenImagesAndInferenceResultsBySession(
                                session.localId
                            )
                        val areSpecimensUploaded = !specimens.any { specimen ->
                            specimen.specimenImagesAndInferenceResults.any { (image, _) ->
                                image.metadataUploadStatus != UploadStatus.COMPLETED || image.imageUploadStatus != UploadStatus.COMPLETED
                            }
                        }

                        if (isSessionUploaded && areSpecimensUploaded) continue

                        workManagerRepository.enqueueSessionUpload(session.localId, site.id)
                    }
                }

                is CompleteSessionListAction.UpdateSearchQuery -> {
                    _state.update { it.copy(searchQuery = action.searchQuery) }
                }
            }
        }
    }
}
