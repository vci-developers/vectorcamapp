package com.vci.vectorcamapp.core.presentation.tutorial

import androidx.compose.runtime.compositionLocalOf

val LocalTutorialManager = compositionLocalOf<TutorialManager> {
    error("No TutorialManager provided")
}
