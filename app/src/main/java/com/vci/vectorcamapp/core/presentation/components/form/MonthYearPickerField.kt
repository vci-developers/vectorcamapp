package com.vci.vectorcamapp.core.presentation.components.form

import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val MONTH_NAMES = (1..12).map { month ->
    LocalDate.of(2000, month, 1).month.getDisplayName(TextStyle.FULL, Locale.getDefault())
}.toTypedArray()

/** Parses a "MM" zero-padded string to a 0-based month index, or null. */
private fun parseCycle(cycle: String?): Int? {
    if (cycle.isNullOrBlank()) return null
    return cycle.toIntOrNull()?.minus(1)?.takeIf { it in 0..11 }
}

/** Formats a 1-based month number to a full month name like "January". */
private fun formatCycleDisplay(month: Int): String =
    LocalDate.of(2000, month, 1).month.getDisplayName(TextStyle.FULL, Locale.getDefault())

@Composable
fun MonthYearPickerField(
    selectedCycle: String?,
    onCycleSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    val parsedIndex = remember(selectedCycle) { parseCycle(selectedCycle) }
    val displayText = remember(parsedIndex) {
        parsedIndex?.let { formatCycleDisplay(it + 1) }
    }

    var pickerMonthIndex by remember(parsedIndex) {
        mutableIntStateOf(parsedIndex ?: (LocalDate.now().monthValue - 1))
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraExtraSmall),
        modifier = Modifier.fillMaxWidth()
    ) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colors.textSecondary,
            )
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colors.cardBackground,
                    shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)
                )
                .border(
                    width = MaterialTheme.dimensions.borderThicknessThick,
                    color = MaterialTheme.colors.primary,
                    shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)
                )
                .heightIn(min = MaterialTheme.dimensions.componentHeightMedium)
                .clickable { showDialog = true }
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.dimensions.paddingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayText ?: "Select a month",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (displayText != null) MaterialTheme.colors.textPrimary
                    else MaterialTheme.colors.textSecondary
                )
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = "Calendar",
                    tint = MaterialTheme.colors.icon,
                    modifier = Modifier.size(MaterialTheme.dimensions.iconSizeExtraLarge)
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    onCycleSelected("%02d".format(pickerMonthIndex + 1))
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text(
                    text = label ?: "Select Month",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                AndroidView(
                    factory = { ctx ->
                        NumberPicker(ctx).apply {
                            minValue = 0
                            maxValue = 11
                            displayedValues = MONTH_NAMES
                            value = pickerMonthIndex
                            setOnValueChangedListener { _, _, newVal -> pickerMonthIndex = newVal }
                        }
                    },
                    update = { picker ->
                        if (picker.value != pickerMonthIndex) picker.value = pickerMonthIndex
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
