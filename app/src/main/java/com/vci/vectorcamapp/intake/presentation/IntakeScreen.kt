package com.vci.vectorcamapp.intake.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.enums.SessionType
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.camera.BarcodeScannerDialog
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.core.presentation.extensions.displayText
import com.vci.vectorcamapp.core.presentation.util.error.toString
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
    var showRefusalConfirmation by remember { mutableStateOf(false) }
    if (showRefusalConfirmation && state.hasDeclinedConsent && !state.isSubmitting) {
        AlertDialog(
            onDismissRequest = { showRefusalConfirmation = false },
            containerColor = MaterialTheme.colors.cardBackground,
            shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
            title = {
                Text(stringResource(R.string.intake_title_submit_household),
                    color = MaterialTheme.colors.textPrimary)
            },
            text = {
                Text(stringResource(R.string.intake_body_confirm_refusal),
                    color = MaterialTheme.colors.textPrimary)
            },
            confirmButton = {
                ActionButton(
                    label = stringResource(R.string.intake_action_submit_household),
                    onClick = {
                        showRefusalConfirmation = false
                        onAction(IntakeAction.ConfirmSubmitWithoutConsent)
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = { showRefusalConfirmation = false }) {
                    Text(stringResource(R.string.intake_action_keep_editing),
                        color = MaterialTheme.colors.textPrimary)
                }
            },
        )
    }
    val showSurveillanceForm = state.canFillForm || state.isSubmitting
    if (state.isSubmitting) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = MaterialTheme.colors.cardBackground,
            shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            confirmButton = {},
            title = {
                Text(
                    text = stringResource(
                        if (state.isCheckingLocation) R.string.intake_title_confirming_location
                        else R.string.intake_title_saving_collection
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colors.textPrimary,
                )
            },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.secondary,
                        modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge),
                    )
                    Text(
                        text = stringResource(R.string.intake_body_please_wait),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.textSecondary,
                    )
                }
            },
        )
    }

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
        if (showSurveillanceForm && state.form != null) {
            item { IntakeProgramFormSection(state = state, onAction = onAction) }
        } else if (showSurveillanceForm) {
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
            val gateMessage = when {
                state.isLoading || state.isSubmitting -> null
                state.isCheckingLocation -> R.string.intake_body_getting_location
                !state.isResumingSession && state.locationError != null -> R.string.intake_body_location_error
                !state.isResumingSession && state.distanceToSiteMeters != null && !state.isWithinSiteRadius ->
                    R.string.intake_body_outside_site_radius
                !state.canFillForm -> R.string.intake_error_site_proximity_required
                !state.hasHouseholdHeadConsent && !state.hasDeclinedConsent -> R.string.intake_error_consent_required
                else -> null
            }
            if (gateMessage != null) {
                Text(
                    text = when (gateMessage) {
                        R.string.intake_body_location_error -> stringResource(
                            gateMessage, requireNotNull(state.locationError).toString(context)
                        )
                        R.string.intake_body_outside_site_radius -> stringResource(
                            gateMessage, requireNotNull(state.distanceToSiteMeters) / 1000
                        )
                        else -> stringResource(gateMessage)
                    },
                    color = if (state.isCheckingLocation) MaterialTheme.colors.textSecondary
                        else MaterialTheme.colors.error,
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimensions.paddingMedium)
                )
            }
            ActionButton(
                label = if (state.hasDeclinedConsent) {
                    stringResource(R.string.intake_action_submit_household)
                } else if (!state.isResumingSession) {
                    stringResource(R.string.intake_action_save_collection)
                } else {
                    stringResource(R.string.intake_action_begin_imaging, state.session.type.displayText(context))
                },
                onClick = {
                    if (state.hasDeclinedConsent) showRefusalConfirmation = true
                    else onAction(IntakeAction.SubmitIntakeForm)
                },
                enabled = state.canFillForm && (state.hasHouseholdHeadConsent || state.hasDeclinedConsent),
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)
            )
        }
    }

    if (isBarcodeScannerVisible) {
        BarcodeScannerDialog(
            title = stringResource(R.string.intake_title_scan_hardware_id),
            description = stringResource(R.string.intake_body_scan_hardware_id),
            onBarcodeScanned = { hardwareId ->
                onAction(IntakeAction.EnterHardwareId(hardwareId))
                isBarcodeScannerVisible = false
            },
            onDismiss = { isBarcodeScannerVisible = false },
            transformValue = { value ->
                value.trim().takeIf { trimmed ->
                    trimmed.isNotEmpty() && trimmed.length <= MAX_HARDWARE_ID_LENGTH &&
                        trimmed.all { it.code <= MAX_HARDWARE_ID_CHARACTER_CODE || it.isWhitespace() }
                }
            },
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

private const val MAX_HARDWARE_ID_LENGTH = 200
private const val MAX_HARDWARE_ID_CHARACTER_CODE = 255

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
