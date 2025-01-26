package com.vci.vectorcamapp.permission.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PermissionViewModel : ViewModel() {
    private val _state = MutableStateFlow(PermissionState())
    val state = _state.asStateFlow()

    private val _events = Channel<PermissionEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: PermissionAction) {
        viewModelScope.launch {
            when (action) {
                PermissionAction.RequestPermissions -> {
                    _state.update { it.copy(isLoading = true) }
                    _events.send(PermissionEvent.LaunchPermissionRequest)
                }

                PermissionAction.OpenAppSettings -> {
                    _events.send(PermissionEvent.NavigateToAppSettings)
                }

                PermissionAction.OpenLocationSettings -> {
                    _events.send(PermissionEvent.NavigateToLocationSettings)
                }

                is PermissionAction.UpdatePermissionStatus -> {
                    _state.update {
                        it.copy(
                            allGranted = action.allGranted, isLoading = false
                        )
                    }
                }

                is PermissionAction.UpdateGpsStatus -> {
                    _state.update {
                        it.copy(
                            isGpsEnabled = action.isGpsEnabled
                        )
                    }
                }
            }
        }
    }
}
