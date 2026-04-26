package com.vci.vectorcamapp.complete_session.list.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.model.helpers.SessionUploadProgress
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenImageRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import com.vci.vectorcamapp.core.presentation.util.search.SearchUtils
import com.vci.vectorcamapp.ui.extensions.displayText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val specimenRepository: SpecimenRepository,
    private val specimenImageRepository: SpecimenImageRepository,
    private val workManagerRepository: WorkManagerRepository,
    errorMessageEmitter: ErrorMessageEmitter,
) : CoreViewModel(errorMessageEmitter) {

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
                            specimenImageRepository.observeUploadedMetadataCountForSession(sessionId),
                            specimenImageRepository.observeUploadedImageCountForSession(sessionId),
                            specimenImageRepository.observeFailedImageCountForSession(sessionId),
                            workManagerRepository.observeIsSessionActivelyUploading(sessionId),
                        ) { uploadedMetadataCount, uploadedImageCount, failedImageCount, isUploading ->
                            sessionAndSite to SessionUploadProgress(
                                uploadedMetadataCount = uploadedMetadataCount,
                                uploadedImageCount = uploadedImageCount,
                                totalCount = specimenImageRepository.getTotalCountForSession(
                                    sessionId
                                ),
                                isUploading = isUploading,
                                failedImageCount = failedImageCount
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
            sessionAndSiteToUploadProgress.filter { (sessionAndSite, progress) ->
                val session = sessionAndSite.session
                val site = sessionAndSite.site
                val fieldsForSearch = buildList {
                    add(session.collectorName)
                    add(session.collectorTitle)
                    add(session.type.name)
                    add(site.district)
                    add(site.subCounty)
                    add(site.parish)
                    add(site.villageName)
                    add(site.houseNumber)
                    add(getSessionUploadStatus(progress))
                    site.locationHierarchy?.values?.forEach {
                        if (it.isNotEmpty()) {
                            add(it)
                        }
                    }
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

                CompleteSessionListAction.ShowSearchTooltipDialog -> {
                    _state.update { it.copy(isSearchTooltipVisible = true) }
                }
                CompleteSessionListAction.HideSearchTooltipDialog -> {
                    _state.update { it.copy(isSearchTooltipVisible = false) }
                }
            }
        }
    }

    private fun getSessionUploadStatus(sessionUploadProgress: SessionUploadProgress): String {
        val isComplete = sessionUploadProgress.totalCount == 0 || sessionUploadProgress.uploadedImageCount == sessionUploadProgress.totalCount

        return when {
            sessionUploadProgress.failedImageCount > 0 -> UploadStatus.FAILED.displayText(context)
            isComplete -> UploadStatus.COMPLETED.displayText(context)
            sessionUploadProgress.isUploading -> UploadStatus.IN_PROGRESS.displayText(context)
            else -> UploadStatus.NOT_STARTED.displayText(context)
        }
    }
}
