package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.tooltip.Tooltip
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
internal fun CollectionMethodField(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
) {
    val otherLabel = IntakeDropdownOptions.CollectionMethodOption.OTHER.label
    val isOther = state.session.collectionMethod.startsWith(otherLabel, ignoreCase = true)
    val selected = if (isOther) {
        IntakeDropdownOptions.CollectionMethodOption.OTHER
    } else {
        IntakeDropdownOptions.CollectionMethodOption.entries.firstOrNull {
            it.label == state.session.collectionMethod
        }
    }

    DropdownField(
        label = stringResource(R.string.intake_label_collection_method),
        required = true,
        enabled = !state.isCollectionMethodLocked,
        options = IntakeDropdownOptions.CollectionMethodOption.entries,
        selectedOption = selected,
        onOptionSelected = { onAction(IntakeAction.UpdateCollectionMethod(it.label)) },
        error = state.intakeErrors.collectionMethod,
        modifier = Modifier.fillMaxWidth(),
    ) { collectionMethod ->
        DropdownOptionLabel(text = collectionMethod.label)
    }

    if (state.isCollectionMethodLocked) {
        Text(
            text = stringResource(R.string.intake_body_method_locked),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colors.warning,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Tooltip(
        isVisible = state.isCollectionMethodTooltipVisible,
        onClick = { onAction(IntakeAction.ShowCollectionMethodTooltipDialog) },
        onDismiss = { onAction(IntakeAction.HideCollectionMethodTooltipDialog) },
        buttonText = stringResource(R.string.intake_action_learn_more_methods),
    ) {
        CollectionMethodTooltipContent()
    }

    if (isOther) {
        OtherLabeledField(
            label = stringResource(R.string.intake_label_other_collection_method),
            prefix = otherLabel,
            value = state.session.collectionMethod,
            error = state.intakeErrors.collectionMethod,
            onValueChange = { onAction(IntakeAction.UpdateCollectionMethod("$otherLabel $it")) },
        )
    }
}

@Composable
private fun CollectionMethodTooltipContent() {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)) {
        Text(
            text = stringResource(R.string.intake_title_collection_methods),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = MaterialTheme.dimensions.paddingSmall),
        )
        CollectionMethodTooltipRow(
            title = stringResource(R.string.intake_title_cdc_light_trap),
            description = stringResource(R.string.intake_body_cdc_light_trap),
            iconPainter = painterResource(id = R.drawable.ic_light_trap),
            iconDescription = stringResource(R.string.intake_content_description_cdc_light_trap),
        )
        CollectionMethodTooltipRow(
            title = stringResource(R.string.intake_title_human_landing_catch),
            description = stringResource(R.string.intake_body_human_landing_catch),
            iconPainter = painterResource(id = R.drawable.ic_human),
            iconDescription = stringResource(R.string.intake_content_description_human_landing_catch),
        )
        CollectionMethodTooltipRow(
            title = stringResource(R.string.intake_title_pyrethrum_spray_catch),
            description = stringResource(R.string.intake_body_pyrethrum_spray_catch),
            iconPainter = painterResource(id = R.drawable.ic_spray),
            iconDescription = stringResource(R.string.intake_content_description_pyrethrum_spray_catch),
        )
    }
}
