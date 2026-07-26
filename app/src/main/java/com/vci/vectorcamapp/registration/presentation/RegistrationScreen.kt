package com.vci.vectorcamapp.registration.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.form.DatePickerField
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.gestures.PullToRefresh
import com.vci.vectorcamapp.registration.domain.model.RegistrationDropdownOptions
import com.vci.vectorcamapp.registration.presentation.util.RegistrationTestTags
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.customShadow
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import kotlin.math.roundToInt

@Composable
fun RegistrationScreen(
    state: RegistrationState, onAction: (RegistrationAction) -> Unit, modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        PullToRefresh(
            isRefreshing = state.isLoadingPrograms,
            onRefresh = { onAction(RegistrationAction.RefreshPrograms) },
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(R.drawable.registration_background),
                contentDescription = "Mosquito background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colors.cardBackground
                ),
                shape = RoundedCornerShape(
                    topStart = MaterialTheme.dimensions.cornerRadiusMedium,
                    topEnd = MaterialTheme.dimensions.cornerRadiusMedium
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = MaterialTheme.dimensions.paddingExtraLarge,
                            vertical = MaterialTheme.dimensions.paddingExtraExtraLarge
                        )
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingLarge)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        Text(
                            text = "Register",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colors.textPrimary
                        )

                        Text(
                            text = "Select your affiliated program",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                    ) {
                        DropdownField(
                            label = "Program",
                            options = state.programs,
                            selectedOption = state.selectedProgram,
                            onOptionSelected = { onAction(RegistrationAction.SelectProgram(it)) },
                            menuTestTag = RegistrationTestTags.PROGRAM_DROPDOWN,
                            menuItemTestTagPrefix = RegistrationTestTags.PROGRAM_OPTION,
                            modifier = Modifier
                                .customShadow(
                                    color = Color.Black.copy(alpha = 0.1f),
                                    blurRadius = MaterialTheme.dimensions.shadowBlurMedium,
                                    spread = MaterialTheme.dimensions.shadowBlurSmall,
                                    cornerRadius = MaterialTheme.dimensions.cornerRadiusSmall,
                                )
                                .height(MaterialTheme.dimensions.componentHeightExtraExtraLarge),
                        ) { program ->
                            Column(
                                verticalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = program.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colors.textPrimary
                                )
                                Text(
                                    text = program.country,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colors.textSecondary
                                )
                            }
                        }

                        TextEntryField(
                            label = "Collector Name",
                            value = state.collector.name,
                            onValueChange = { onAction(RegistrationAction.EnterCollectorName(it)) },
                            singleLine = true,
                            error = state.registrationErrors.collectorName
                        )

                        DropdownField(
                            label = "Collector Title",
                            options = RegistrationDropdownOptions.CollectorTitleOption.entries,
                            selectedOption = RegistrationDropdownOptions.CollectorTitleOption.entries.firstOrNull { it.label == state.collector.title },
                            onOptionSelected = { option ->
                                onAction(RegistrationAction.EnterCollectorTitle(option.label))
                            },
                            error = state.registrationErrors.collectorTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MaterialTheme.dimensions.componentHeightLarge)
                        ) { option ->
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.textPrimary
                            )
                        }

                        DatePickerField(
                            label = "When were you last trained?",
                            selectedDateInMillis = state.collector.lastTrainedOn,
                            onDateSelected = { onAction(RegistrationAction.EnterCollectorLastTrainedOn(it)) },
                            error = state.registrationErrors.collectorLastTrainedOn,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionButton(
                        label = "Confirm",
                        onClick = { onAction(RegistrationAction.ConfirmRegistration) },
                        enabled = state.selectedProgram != null &&
                                state.collector.name.isNotBlank() &&
                                state.collector.title.isNotBlank() &&
                                state.collector.lastTrainedOn != 0L &&
                                !state.isLoading,
                        testTag = RegistrationTestTags.CONFIRM_PROGRAM_BUTTON,
                        modifier = Modifier.height(MaterialTheme.dimensions.componentHeightMedium)
                    )
                }
            }
        }

        if (state.isProgramAccessCodeDialogVisible) {
            var isProgramAccessCodeVisible by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { onAction(RegistrationAction.DismissProgramAccessCodeDialog) },
                title = {
                    Text(text = "Enter Program Access Code")
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        TextEntryField(
                            value = state.programAccessCodeInput,
                            onValueChange = { onAction(RegistrationAction.EnterProgramAccessCode(it)) },
                            label = "Access Code",
                            singleLine = true,
                            error = state.programAccessCodeError,
                            visualTransformation = if (isProgramAccessCodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isProgramAccessCodeVisible = !isProgramAccessCodeVisible }) {
                                    Icon(
                                        painter = painterResource(
                                            if (isProgramAccessCodeVisible) R.drawable.ic_visibility_off
                                            else R.drawable.ic_visibility
                                        ),
                                        contentDescription = if (isProgramAccessCodeVisible) "Hide access code" else "Show access code"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onAction(RegistrationAction.SubmitProgramAccessCode) },
                        enabled = state.programAccessCodeInput.isNotBlank() && !state.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.buttonText
                        )
                    ) {
                        Text(
                            text = "Confirm",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onAction(RegistrationAction.DismissProgramAccessCodeDialog) }
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colors.textSecondary,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize
                        )
                    }
                },
                containerColor = MaterialTheme.colors.cardBackground
            )
        }

        // Show after access-code login succeeds (dialog dismissed) while seeding/downloading.
        if (state.isLoading && !state.isProgramAccessCodeDialogVisible) {
            RegistrationLoadingDialog(
                loadingPhase = state.loadingPhase ?: RegistrationLoadingPhase.DOWNLOADING_MODEL,
                modelDownloadProgress = state.modelDownloadProgress,
                downloadedBytes = state.modelDownloadBytes,
                totalBytes = state.modelDownloadTotalBytes,
            )
        }
    }
}

@Composable
private fun RegistrationLoadingDialog(
    loadingPhase: RegistrationLoadingPhase,
    modelDownloadProgress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colors.cardBackground
                ),
                shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimensions.paddingExtraLarge)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.dimensions.paddingExtraLarge),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (loadingPhase) {
                        RegistrationLoadingPhase.DOWNLOADING_MODEL -> {
                            val progress = modelDownloadProgress.coerceIn(0f, 1f)
                            val percent = (progress * 100).roundToInt().coerceIn(0, 100)
                            Text(
                                text = "Downloading ML model",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colors.primary,
                                trackColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                            )
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colors.textPrimary
                            )
                            Text(
                                text = if (totalBytes > 0L) {
                                    "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
                                } else {
                                    "Fetching model details…"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Please keep the app open while the model downloads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        RegistrationLoadingPhase.SETTING_UP_PROGRAM -> {
                            Text(
                                text = "Setting up program",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            // Keep a linear bar only — never a circular spinner.
                            LinearProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colors.primary,
                                trackColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                            )
                            Text(
                                text = "Model ready — fetching program data…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}

@PreviewLightDark
@Composable
fun RegistrationScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            RegistrationScreen(
                state = RegistrationState(),
                onAction = { },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
