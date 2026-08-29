package com.vci.vectorcamapp.landing.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.landing.presentation.components.LandingActionTile
import com.vci.vectorcamapp.landing.presentation.components.LandingSection
import com.vci.vectorcamapp.landing.presentation.util.LandingTestTags
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun LandingScreen(
    state: LandingState,
    onAction: (LandingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenHeader(
        title = stringResource(R.string.landing_title_welcome),
        subtitle = stringResource(R.string.landing_label_program, state.enrolledProgram.name),
        modifier = modifier.testTag(LandingTestTags.SCREEN),
        trailingIcon = {
            IconButton(onClick = { onAction(LandingAction.OpenSettings) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.landing_content_description_settings),
                    modifier = Modifier
                        .size(MaterialTheme.dimensions.iconSizeLarge)
                )
            }
        }
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                modifier = Modifier.padding(top = MaterialTheme.dimensions.spacingMedium)
            ) {
                LandingSection(
                    title = stringResource(R.string.landing_title_section_imaging),
                    testTag = LandingTestTags.SECTION_IMAGING
                ) {
                    LandingActionTile(
                        title = stringResource(R.string.landing_title_new_surveillance),
                        description = stringResource(R.string.landing_body_new_surveillance),
                        icon = painterResource(R.drawable.ic_specimen),
                        onClick = { onAction(LandingAction.StartNewSurveillanceSession) },
                        testTag = LandingTestTags.TILE_NEW_SURVEILLANCE
                    )
                }

                LandingSection(
                    title = stringResource(R.string.landing_title_section_library),
                    testTag = LandingTestTags.SECTION_LIBRARY
                ) {
                    LandingActionTile(
                        title = stringResource(R.string.landing_title_incomplete_sessions),
                        description = stringResource(R.string.landing_body_incomplete_sessions),
                        icon = painterResource(R.drawable.ic_minus_circle),
                        onClick = { onAction(LandingAction.ViewIncompleteSessions) },
                        badgeCount = state.incompleteSessionsCount,
                        testTag = LandingTestTags.TILE_INCOMPLETE,
                    )

                    LandingActionTile(
                        title = stringResource(R.string.landing_title_complete_sessions),
                        description = stringResource(R.string.landing_body_complete_sessions),
                        icon = painterResource(R.drawable.ic_complete),
                        onClick = { onAction(LandingAction.ViewCompleteSessions) },
                        testTag = LandingTestTags.TILE_COMPLETE
                    )
                }
            }
        }
    }

    if (state.showResumeDialog) {
        AlertDialog(
            onDismissRequest = { onAction(LandingAction.DismissResumePrompt) },
            title = {
                Text(
                    text = stringResource(R.string.landing_title_resume_dialog),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.landing_body_resume_dialog),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { onAction(LandingAction.ResumeSession) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colors.successConfirm
                    ),
                    modifier = Modifier.testTag(LandingTestTags.RESUME_CONFIRM)
                ) {
                    Text(
                        text = stringResource(R.string.landing_action_resume_dialog_confirm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.buttonText
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(LandingAction.DismissResumePrompt) },
                    modifier = Modifier.testTag(LandingTestTags.RESUME_DISMISS)
                ) {
                    Text(
                        text = stringResource(R.string.landing_action_resume_dialog_dismiss),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.error
                    )
                }
            },
            modifier = Modifier.testTag(LandingTestTags.RESUME_DIALOG)
        )
    }
}

@PreviewLightDark
@Composable
fun LandingScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            LandingScreen(
                state = LandingState(),
                onAction = {},
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
