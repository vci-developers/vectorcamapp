package com.vci.vectorcamapp.imaging.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vci.vectorcamapp.core.domain.util.onError
import com.vci.vectorcamapp.core.domain.util.onSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImagingViewModel : ViewModel() {

    private val _state = MutableStateFlow(ImagingState())
    val state: StateFlow<ImagingState> = _state.asStateFlow()

    private val _events = Channel<ImagingEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ImagingAction) {
        viewModelScope.launch {
            when (action) {
                is ImagingAction.UpdateSpecimenId -> {
                    _state.update {
                        it.copy(currentSpecimenId = action.specimenId)
                    }
                }

                is ImagingAction.UpdateDetection -> {
                    _state.update {
                        it.copy(detection = action.detection)
                    }
                }

                is ImagingAction.CaptureComplete -> {
                    action.result.onSuccess { bitmap ->
                        _state.update {
                            it.copy(
                                currentImage = bitmap,
                            )
                        }
                    }.onError { error ->
                        _events.send(ImagingEvent.DisplayImagingError(error))
                    }
                }

                ImagingAction.RetakeImage -> {
                    _state.update {
                        it.copy(
                            currentSpecimenId = "",
                            currentImage = null,
                            detection = null,
                        )
                    }
                }
            }
        }
    }
}
