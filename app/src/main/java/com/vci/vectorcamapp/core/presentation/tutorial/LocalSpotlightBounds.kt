package com.vci.vectorcamapp.core.presentation.tutorial

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Rect

val LocalSpotlightBounds = compositionLocalOf<MutableState<Rect?>> {
    error("No SpotlightBounds provided")
}
