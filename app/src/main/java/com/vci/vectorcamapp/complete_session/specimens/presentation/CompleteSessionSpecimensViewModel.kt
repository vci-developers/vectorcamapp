package com.vci.vectorcamapp.complete_session.specimens.presentation

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.complete_session.domain.util.CompleteSessionError
import com.vci.vectorcamapp.core.domain.repository.SessionRepository
import com.vci.vectorcamapp.core.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CompleteSessionSpecimensViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(CompleteSessionSpecimensState())
    val state: StateFlow<CompleteSessionSpecimensState> = _state

    fun onAction(action: CompleteSessionSpecimensAction) {
        when (action) {
            is CompleteSessionSpecimensAction.LoadSession -> loadSession(action.sessionId)
        }
    }

    private fun loadSession(sessionId: UUID) {
        viewModelScope.launch {
            val sessionWithSpecimens = sessionRepository.getSessionWithSpecimens(sessionId)

            if (sessionWithSpecimens == null) {
                emitError(CompleteSessionError.SESSION_NOT_FOUND, context)
                return@launch
            } else {
                _state.update {
                    it.copy(
                        session = sessionWithSpecimens.session,
                        specimens = sessionWithSpecimens.specimens,
                    )
                }

                if (sessionWithSpecimens.specimens.isEmpty()) {
                    emitError(CompleteSessionError.NO_SPECIMENS, context)
                }
            }
        }
    }
}
