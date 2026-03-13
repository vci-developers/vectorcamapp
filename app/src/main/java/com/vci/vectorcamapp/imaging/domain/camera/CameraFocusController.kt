package com.vci.vectorcamapp.imaging.domain.camera

import androidx.compose.ui.geometry.Offset

interface CameraFocusController {
    fun focusAt(offset: Offset)
    fun cancelFocus()
    fun setFocusDistance(normalizedDistance: Float?)
}
