package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.Collector
import com.vci.vectorcamapp.core.presentation.components.form.DatePickerField
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import java.util.UUID

@Composable
internal fun IntakeGeneralInfoSection(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
    onScanHardwareId: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IntakeTile(
        title = stringResource(R.string.intake_title_general_info),
        iconPainter = painterResource(R.drawable.ic_info),
        iconDescription = stringResource(R.string.intake_content_description_general_info),
        modifier = modifier,
    ) {
        CollectorField(state = state, onAction = onAction)
        HardwareIdField(state = state, onAction = onAction, onScanHardwareId = onScanHardwareId)
        DatePickerField(
            label = stringResource(R.string.intake_label_collection_date),
            required = true,
            selectedDateInMillis = state.session.collectionDate,
            onDateSelected = { onAction(IntakeAction.PickCollectionDate(it)) },
            error = state.intakeErrors.collectionDate,
            modifier = Modifier.fillMaxWidth(),
        )
        CollectionMethodField(state = state, onAction = onAction)
        SpecimenConditionField(state = state, onAction = onAction)
    }
}

@Composable
private fun CollectorField(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
) {
    val selectedCollector = if (state.isCurrentCollectorMissing) {
        Collector(
            id = UUID.randomUUID(),
            name = state.session.collectorName,
            title = state.session.collectorTitle,
            lastTrainedOn = state.session.collectorLastTrainedOn,
        )
    } else {
        state.allCollectors.firstOrNull { collector ->
            collector.name == state.session.collectorName && collector.title == state.session.collectorTitle
        }
    }

    DropdownField(
        label = stringResource(R.string.intake_label_collector),
        required = true,
        options = state.allCollectors,
        selectedOption = selectedCollector,
        onOptionSelected = { onAction(IntakeAction.SelectCollector(it)) },
        error = state.intakeErrors.collector,
        modifier = Modifier.fillMaxWidth(),
    ) { collector ->
        DropdownOptionLabel(text = collector.name + ", " + collector.title)
    }

    if (state.isCurrentCollectorMissing) {
        MissingCollectorCard(
            name = state.session.collectorName,
            title = state.session.collectorTitle,
            onRegister = { onAction(IntakeAction.RegisterMissingCollector) },
        )
    }
}

@Composable
private fun HardwareIdField(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
    onScanHardwareId: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            TextEntryField(
                label = stringResource(R.string.intake_label_hardware_id),
                value = state.session.hardwareId.orEmpty(),
                onValueChange = { onAction(IntakeAction.EnterHardwareId(it)) },
                singleLine = true,
            )
        }
        IconButton(onClick = onScanHardwareId) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = stringResource(R.string.intake_content_description_scan_hardware_id),
                tint = MaterialTheme.colors.icon,
            )
        }
    }
}
