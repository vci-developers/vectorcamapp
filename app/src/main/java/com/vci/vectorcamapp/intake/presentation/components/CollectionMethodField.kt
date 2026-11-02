package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState
import com.vci.vectorcamapp.ui.extensions.colors

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
