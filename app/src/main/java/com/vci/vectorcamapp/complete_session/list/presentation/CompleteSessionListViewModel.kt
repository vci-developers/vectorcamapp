package com.vci.vectorcamapp.complete_session.list.presentation

import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.domain.repository.SpecimenRepository
import com.vci.vectorcamapp.core.domain.repository.WorkManagerRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CompleteSessionListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val specimenRepository: SpecimenRepository,
    private val workManagerRepository: WorkManagerRepository
) : CoreViewModel() {

    private val _events = Channel<CompleteSessionListEvent>()
    val events = _events.receiveAsFlow()

    private val sessionsAndSites = sessionRepository.observeCompleteSessionsAndSites()

    private val activeUploadingSessions = sessionsAndSites
        .flatMapLatest { sessions ->
            val sessionIds = sessions.map { it.session.localId }
            if (sessionIds.isEmpty()) {
                flowOf(emptySet())
            } else {
                workManagerRepository.observeActiveUploadingSessions(sessionIds)
                    .map { it.toSet() }
            }
        }

    val state = combine(
        sessionsAndSites,
        activeUploadingSessions
    ) { sessionsAndSites, activeUploadingSessions ->
        CompleteSessionListState(
            sessionsAndSites = sessionsAndSites,
            activeUploadingSessions = activeUploadingSessions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CompleteSessionListState())


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
            }
        }
    }
}
