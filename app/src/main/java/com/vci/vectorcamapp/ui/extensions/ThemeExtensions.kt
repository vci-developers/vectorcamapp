package com.vci.vectorcamapp.ui.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.vci.vectorcamapp.ui.theme.Dimension
import com.vci.vectorcamapp.ui.theme.LocalDimensions

val MaterialTheme.dimensions: Dimension
    @Composable
    get() = LocalDimensions.current
