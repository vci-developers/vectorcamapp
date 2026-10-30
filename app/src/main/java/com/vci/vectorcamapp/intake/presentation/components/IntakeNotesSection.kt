package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState

@Composable
internal fun IntakeNotesSection(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    IntakeTile(
        title = stringResource(R.string.intake_title_additional_notes),
        iconPainter = painterResource(id = R.drawable.ic_notes),
        iconDescription = stringResource(R.string.intake_content_description_additional_notes),
        modifier = modifier,
    ) {
        TextEntryField(
            label = stringResource(R.string.intake_label_notes),
            value = state.session.notes,
            onValueChange = { onAction(IntakeAction.EnterNotes(it)) },
            placeholder = stringResource(R.string.intake_placeholder_notes),
            maxCharacters = 1000,
        )
    }
}
