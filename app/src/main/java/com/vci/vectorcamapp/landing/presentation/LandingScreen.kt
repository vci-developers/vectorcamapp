package com.vci.vectorcamapp.landing.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.logging.CrashyContext
import com.vci.vectorcamapp.core.presentation.components.button.ClickTracking
import com.vci.vectorcamapp.core.presentation.components.button.TrackedIconButton
import com.vci.vectorcamapp.core.presentation.components.button.TrackedTextButton
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
        title = "Welcome to VectorCam!",
        subtitle = "Program: ${state.enrolledProgram.name}",
        modifier = modifier.testTag(LandingTestTags.SCREEN),
        trailingIcon = {
            TrackedIconButton(
                message = "Landing: Open settings clicked",
                crashyContext = CrashyContext(
                    screen = "LandingScreen",
                    feature = "Header",
                    action = "OpenSettings"
                ),
                onClick = { onAction(LandingAction.OpenSettings) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings Icon",
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
                    title = "Imaging",
                    testTag = LandingTestTags.SECTION_IMAGING
                ) {
                    LandingActionTile(
                        title = "New Surveillance Session",
                        description = "Begin a new household visit and capture mosquito images.",
                        icon = painterResource(R.drawable.ic_specimen),
                        onClick = {
                            ClickTracking.trackAndInvoke(
                                context = CrashyContext(
                                    screen = "LandingScreen",
                                    feature = "Imaging",
                                    action = "StartNewSurveillanceSession"
                                ),
                                message = "Landing: Start new surveillance session",
                                category = "ui.click",
                                onClick = { onAction(LandingAction.StartNewSurveillanceSession) }
                            )
                        },
                        testTag = LandingTestTags.TILE_NEW_SURVEILLANCE
                    )
                }

                LandingSection(
                    title = "Library",
                    testTag = LandingTestTags.SECTION_LIBRARY
                ) {
                    LandingActionTile(
                        title = "View Sessions in Progress",
                        description = "Resume and complete any unfinished sessions.",
                        icon = painterResource(R.drawable.ic_minus_circle),
                        onClick = {
                            ClickTracking.trackAndInvoke(
                                context = CrashyContext(
                                    screen = "LandingScreen",
                                    feature = "Library",
                                    action = "ViewIncompleteSessions"
                                ),
                                message = "Landing: View incomplete sessions",
                                category = "ui.click",
                                onClick = { onAction(LandingAction.ViewIncompleteSessions) }
                            )
                        },
                        badgeCount = state.incompleteSessionsCount,
                        testTag = LandingTestTags.TILE_INCOMPLETE,
                    )

                    LandingActionTile(
                        title = "View Complete Sessions",
                        description = "Review fully completed sessions and uploaded data.",
                        icon = painterResource(R.drawable.ic_complete),
                        onClick = {
                            ClickTracking.trackAndInvoke(
                                context = CrashyContext(
                                    screen = "LandingScreen",
                                    feature = "Library",
                                    action = "ViewCompleteSessions"
                                ),
                                message = "Landing: View complete sessions",
                                category = "ui.click",
                                onClick = { onAction(LandingAction.ViewCompleteSessions) }
                            )
                        },
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
                    text = "Resume unfinished session?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "You have a surveillance session in progress. Resume where you left off?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TrackedTextButton(
                    label = "Yes, resume",
                    crashyContext = CrashyContext(
                        screen = "LandingScreen",
                        feature = "ResumeDialog",
                        action = "ResumeSession"
                    ),
                    onClick = { onAction(LandingAction.ResumeSession) },
                    modifier = Modifier.testTag(LandingTestTags.RESUME_CONFIRM)
                ) {
                    Text(
                        text = "Yes, resume",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colors.successConfirm
                    )
                }
            },
            dismissButton = {
                TrackedTextButton(
                    label = "No, start new",
                    crashyContext = CrashyContext(
                        screen = "LandingScreen",
                        feature = "ResumeDialog",
                        action = "DismissResumePrompt"
                    ),
                    onClick = { onAction(LandingAction.DismissResumePrompt) },
                    modifier = Modifier.testTag(LandingTestTags.RESUME_DISMISS)
                ) {
                    Text(
                        text = "No, start new",
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
