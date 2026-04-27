package com.vci.vectorcamapp.core.presentation

import androidx.compose.runtime.compositionLocalOf
import com.vci.vectorcamapp.core.logging.CrashyContext

/**
 * CompositionLocal for the current screen/feature [CrashyContext].
 * [VectorcamappTheme] provides null by default; set via a screen-level
 * [androidx.compose.runtime.CompositionLocalProvider] (e.g. in each screen's root)
 * so [TrackedActionButton], [TrackedTextButton], and [TrackedIconButton] can read it
 * when no explicit [CrashyContext] is passed.
 */
val LocalCrashyContext = compositionLocalOf<CrashyContext?> { null }
