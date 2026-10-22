package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.vci.vectorcamapp.ui.extensions.colors

@Composable
internal fun DropdownOptionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colors.textPrimary,
    )
}
