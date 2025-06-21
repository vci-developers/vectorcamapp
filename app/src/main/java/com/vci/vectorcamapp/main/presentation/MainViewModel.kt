package com.vci.vectorcamapp.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.cache.DeviceCache
import com.vci.vectorcamapp.navigation.Destination
import com.vci.vectorcamapp.permission.presentation.PermissionAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val deviceCache: DeviceCache
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            checkPermissions()
            fetchProgramId()
        }
    }

    private suspend fun fetchProgramId() {
        val programName = deviceCache.getProgramName()
        val destination = if (programName == null) Destination.Registration else Destination.Landing

        _state.update { st ->
            st.copy(
                selectedProgramName = programName,
                startDestination = destination,
                isLoading = false
            )
        }
    }

    private fun checkPermissions() {
        onAction(PermissionAction.RequestPermissions)
    }

    fun onAction(action: PermissionAction) {
        viewModelScope.launch {
            when (action) {
                PermissionAction.RequestPermissions -> {
                    _events.send(MainEvent.LaunchPermissionRequest)
                }
                PermissionAction.OpenAppSettings -> {
                    _events.send(MainEvent.NavigateToAppSettings)
                }
                PermissionAction.OpenLocationSettings -> {
                    _events.send(MainEvent.NavigateToLocationSettings)
                }
                is PermissionAction.UpdatePermissionStatus -> {
                    _state.update { it.copy(allPermissionsGranted = action.allGranted) }
                }
                is PermissionAction.UpdateGpsStatus -> {
                    _state.update { it.copy(isGpsEnabled = action.isGpsEnabled) }
                }
            }
        }
    }
}
