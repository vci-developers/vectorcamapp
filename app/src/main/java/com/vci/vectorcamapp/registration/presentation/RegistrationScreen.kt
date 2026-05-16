package com.vci.vectorcamapp.registration.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
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

@Composable
fun RegistrationScreen(
    state: RegistrationState, onAction: (RegistrationAction) -> Unit, modifier: Modifier = Modifier
) {
    PullToRefresh(
        isRefreshing = state.isLoadingPrograms,
        onRefresh = { onAction(RegistrationAction.RefreshPrograms) },
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(R.drawable.registration_background),
            contentDescription = "Mosquito background",
            contentScale = ContentScale.Crop,
            modifier = modifier
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
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = modifier
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
                        modifier = modifier
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
                            modifier = Modifier
                                .fillMaxWidth()
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
                        modifier = modifier.fillMaxWidth()
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
                    modifier = modifier.height(MaterialTheme.dimensions.componentHeightMedium)
                )
            }
        }

        if (state.isPasswordDialogVisible) {
            var passwordVisible by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { onAction(RegistrationAction.DismissRegistrationPasswordDialog) },
                title = {
                    Text(text = "Enter Program Access Code")
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                    ) {
                        TextEntryField(
                            value = state.registrationPasswordInput,
                            onValueChange = { onAction(RegistrationAction.EnterRegistrationPassword(it)) },
                            label = "Access Code",
                            singleLine = true,
                            error = state.registrationPasswordError,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        painter = painterResource(
                                            if (passwordVisible) R.drawable.ic_visibility_off
                                            else R.drawable.ic_visibility
                                        ),
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onAction(RegistrationAction.SubmitRegistrationPassword) },
                        enabled = state.registrationPasswordInput.isNotBlank() && !state.isLoading,
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
                        onClick = { onAction(RegistrationAction.DismissRegistrationPasswordDialog) }
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
    }
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
