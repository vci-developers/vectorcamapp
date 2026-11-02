package com.vci.vectorcamapp.intake.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.core.presentation.extensions.displayText
import com.vci.vectorcamapp.intake.presentation.components.HardwareIdBarcodeScannerDialog
import com.vci.vectorcamapp.intake.presentation.components.IntakeGeneralInfoSection
import com.vci.vectorcamapp.intake.presentation.components.IntakeGeographicalInfoSection
import com.vci.vectorcamapp.intake.presentation.components.IntakeNotesSection
import com.vci.vectorcamapp.intake.presentation.components.IntakeProgramFormSection
import com.vci.vectorcamapp.intake.presentation.components.IntakeSurveillanceFormSection
import com.vci.vectorcamapp.intake.presentation.components.PracticeSessionWarningBanner
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun IntakeScreen(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isBarcodeScannerVisible by remember { mutableStateOf(false) }

    BackHandler {
        onAction(IntakeAction.ReturnToPreviousScreen)
    }

    ScreenHeader(
        title = stringResource(R.string.intake_title_screen, state.session.type.displayText(context)),
        subtitle = stringResource(R.string.intake_body_subtitle),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = stringResource(R.string.intake_content_description_back),
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable { onAction(IntakeAction.ReturnToPreviousScreen) },
            )
        },
        modifier = modifier,
    ) {
        if (state.session.type == SessionType.PRACTICE) {
            item {
                PracticeWarning(
                    top = MaterialTheme.dimensions.paddingSmall,
                    bottom = MaterialTheme.dimensions.paddingSmall,
                )
            }
        }
        item {
            IntakeGeneralInfoSection(
                state = state,
                onAction = onAction,
                onScanHardwareId = { isBarcodeScannerVisible = true },
            )
        }
        item { IntakeGeographicalInfoSection(state = state, onAction = onAction) }
        if (state.form != null) {
            item { IntakeProgramFormSection(state = state, onAction = onAction) }
        } else {
            state.surveillanceForm?.let { surveillanceForm ->
                item {
                    IntakeSurveillanceFormSection(
                        state = state,
                        surveillanceForm = surveillanceForm,
                        onAction = onAction,
                    )
                }
            }
        }
        item { IntakeNotesSection(state = state, onAction = onAction) }
        if (state.session.type == SessionType.PRACTICE) {
            item {
                PracticeWarning(
                    top = MaterialTheme.dimensions.paddingSmall,
                    bottom = 0.dp,
                )
            }
        }
        item {
            ActionButton(
                label = stringResource(
                    R.string.intake_action_begin_imaging,
                    state.session.type.displayText(context),
                ),
                onClick = { onAction(IntakeAction.SubmitIntakeForm) },
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium),
            )
        }
    }

    if (isBarcodeScannerVisible) {
        HardwareIdBarcodeScannerDialog(
            onBarcodeScanned = { hardwareId ->
                onAction(IntakeAction.EnterHardwareId(hardwareId))
                isBarcodeScannerVisible = false
            },
            onDismiss = { isBarcodeScannerVisible = false },
        )
    }
}

@Composable
private fun PracticeWarning(top: Dp, bottom: Dp) {
    PracticeSessionWarningBanner(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimensions.paddingMedium)
            .padding(top = top, bottom = bottom),
    )
}

@PreviewLightDark
@Composable
fun SurveillanceFormScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            IntakeScreen(
                state = IntakeState(),
                onAction = { },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
