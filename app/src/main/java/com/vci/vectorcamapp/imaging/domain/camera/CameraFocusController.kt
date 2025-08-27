package com.vci.vectorcamapp.imaging.domain.camera

import androidx.compose.ui.geometry.Offset
import com.vci.vectorcamapp.core.domain.model.InferenceResult

interface CameraFocusController {
    fun focusAt(offset: Offset)
    fun cancelFocus()
}
