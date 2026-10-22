package com.vci.vectorcamapp.core.presentation.components.form

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.vci.vectorcamapp.core.domain.util.Error
import com.vci.vectorcamapp.core.presentation.util.error.toString
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun MultiSelectField(
    label: String,
    options: List<String>,
    selectedValues: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: Error? = null,
) {
    val colors = MaterialTheme.colors
    val dimensions = MaterialTheme.dimensions
    val shape = RoundedCornerShape(dimensions.cornerRadiusSmall)
    val distinctOptions = options.distinct()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingExtraExtraSmall),
    ) {
        FormFieldLabel(text = label, required = required)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.cardBackground)
                .border(
                    width = dimensions.borderThicknessThick,
                    color = if (error != null) colors.error else colors.primary,
                    shape = shape,
                ),
        ) {
            distinctOptions.forEachIndexed { index, option ->
                val selected = option in selectedValues
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (selected) colors.primary.copy(alpha = 0.08f) else colors.cardBackground)
                        .toggleable(
                            value = selected,
                            role = Role.Checkbox,
                            onValueChange = { checked ->
                                val updated = if (checked) selectedValues + option else selectedValues - option
                                onSelectionChange(distinctOptions.filter { it in updated })
                            },
                        )
                        .heightIn(min = dimensions.componentHeightMedium)
                        .padding(
                            horizontal = dimensions.paddingMedium,
                            vertical = dimensions.paddingSmall,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.primary,
                            uncheckedColor = colors.textSecondary,
                            checkmarkColor = colors.cardBackground,
                        ),
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (index != distinctOptions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = dimensions.paddingMedium),
                        thickness = dimensions.dividerThicknessThin,
                        color = colors.divider,
                    )
                }
            }
        }
        if (error != null) {
            Text(
                text = error.toString(LocalContext.current),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.error,
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun MultiSelectFieldPreview() {
    VectorcamappTheme {
        Column(modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)) {
            MultiSelectField(
                label = "Select the types of breeding sites",
                options = listOf("Concrete Tank", "Abandoned Tyres", "Open Water Storage Containers", "Other"),
                selectedValues = listOf("Concrete Tank", "Other"),
                onSelectionChange = {},
                required = true,
            )
        }
    }
}
