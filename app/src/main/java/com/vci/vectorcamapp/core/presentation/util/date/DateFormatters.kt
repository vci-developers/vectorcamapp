package com.vci.vectorcamapp.core.presentation.util.date

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun rememberDateFormatter(@StringRes patternResId: Int): SimpleDateFormat {
    val pattern = stringResource(patternResId)
    return remember(pattern) { SimpleDateFormat(pattern, Locale.getDefault()) }
}
