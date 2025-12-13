package com.vci.vectorcamapp.incomplete_session.presentation

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.cache.CurrentSessionCache
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.presentation.CoreViewModel
import com.vci.vectorcamapp.core.presentation.util.search.SearchUtils
import com.vci.vectorcamapp.imaging.domain.repository.CameraRepository
import com.vci.vectorcamapp.incomplete_session.domain.util.IncompleteSessionError
import com.vci.vectorcamapp.incomplete_session.logging.IncompleteSessionSentryLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncompleteSessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val currentSessionCache: CurrentSessionCache,
    private val cameraRepository: CameraRepository
) : CoreViewModel() {

    private val _incompleteSessionsAndSites = sessionRepository.observeIncompleteSessionsAndSites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    private val _state = MutableStateFlow(IncompleteSessionState())

    val state = combine(_incompleteSessionsAndSites, _state) { incompleteSessionsAndSites, currentState ->
        val filteredSessions = if (currentState.searchQuery.isBlank()) {
            incompleteSessionsAndSites
        } else {
            incompleteSessionsAndSites.filter { sessionAndSite ->
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
                }
                SearchUtils.matchesQuery(currentState.searchQuery, fieldsForSearch)
            }
        }
        currentState.copy(sessionAndSites = filteredSessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), IncompleteSessionState())

    private val _events = Channel<IncompleteSessionEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: IncompleteSessionAction) {
        viewModelScope.launch {
            when (action) {
                is IncompleteSessionAction.ResumeSession -> {
                    try {
                        val sessionAndSite = sessionRepository.getSessionAndSiteById(action.sessionId)
                        if (sessionAndSite != null) {
                            currentSessionCache.saveSession(sessionAndSite.session, sessionAndSite.site.id)
                            _events.send(IncompleteSessionEvent.NavigateToIntakeScreen(sessionAndSite.session.type))
                        } else {
                            emitError(IncompleteSessionError.SESSION_NOT_FOUND)
                            IncompleteSessionSentryLogger.logSessionNotFound(Exception(IncompleteSessionError.SESSION_NOT_FOUND.name), action.sessionId)
                        }
                    } catch (e: Exception) {
                        emitError(IncompleteSessionError.SESSION_RETRIEVAL_FAILED)
                        IncompleteSessionSentryLogger.logSessionRetrievalFailure(Exception(IncompleteSessionError.SESSION_RETRIEVAL_FAILED.name, e), action.sessionId)
                    }
                }

                is IncompleteSessionAction.DeleteSession -> {
                    _state.value = _state.value.copy(deleteDialogSessionId = action.sessionId)
                }

                is IncompleteSessionAction.ConfirmDeleteSession -> {
                    try {
                        val sessionAndSite = sessionRepository.getSessionAndSiteById(action.sessionId)
                        if (sessionAndSite != null) {
                            val imageUris = sessionRepository.getImageUrisBySessionId(action.sessionId)
                            imageUris.forEach { uri ->
                                try {
                                    cameraRepository.deleteSavedImage(uri)
                                } catch (e: Exception) {
                                    IncompleteSessionSentryLogger.logImageDeletionFailure(e, action.sessionId, uri)
                                }
                            }

                            val deleteSuccess = sessionRepository.deleteSession(sessionAndSite.session, sessionAndSite.site.id)
                            if (deleteSuccess) {
                                Log.d("IncompleteSessionViewModel", "Successfully deleted session and images for ID: ${action.sessionId}")
                            } else {
                                emitError(IncompleteSessionError.SESSION_DELETION_FAILED)
                                IncompleteSessionSentryLogger.logSessionDeletionFailure(Exception(IncompleteSessionError.SESSION_DELETION_FAILED.name), action.sessionId)
                            }
                        } else {
                            emitError(IncompleteSessionError.SESSION_NOT_FOUND)
                            IncompleteSessionSentryLogger.logSessionNotFound(Exception(IncompleteSessionError.SESSION_NOT_FOUND.name), action.sessionId)
                        }
                    } catch (e: Exception) {
                        emitError(IncompleteSessionError.SESSION_DELETION_FAILED)
                        IncompleteSessionSentryLogger.logSessionDeletionFailure(Exception(IncompleteSessionError.SESSION_DELETION_FAILED.name, e), action.sessionId)
                    } finally {
                        _state.value = _state.value.copy(deleteDialogSessionId = null)
                    }
                }

                is IncompleteSessionAction.DismissDeleteDialog -> {
                    _state.value = _state.value.copy(deleteDialogSessionId = null)
                }

                is IncompleteSessionAction.ReturnToLandingScreen -> {
                    _events.send(IncompleteSessionEvent.NavigateBackToLandingScreen)
                }

                is IncompleteSessionAction.UpdateSearchQuery -> {
                    _state.value = _state.value.copy(searchQuery = action.searchQuery)
                }
                IncompleteSessionAction.ShowSearchTooltipDialog -> {
                    _state.update { it.copy(isSearchTooltipVisible = true) }
                }
                IncompleteSessionAction.HideSearchTooltipDialog -> {
                    _state.update { it.copy(isSearchTooltipVisible = false) }
                }
            }
        }
    }
}
