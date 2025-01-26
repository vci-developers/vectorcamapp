package com.vci.vectorcamapp.landing.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun LandingScreen(
    state: LandingState, onAction: (LandingAction) -> Unit, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp), // Padding for the overall layout
        verticalArrangement = Arrangement.SpaceEvenly, // Spread content vertically
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VectorCam",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.height(16.dp)) // Space below title
            Text(
                text = "Democratizing Vector Surveillance",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center // Align the text content to the center
            )
        }

        // Buttons Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp) // Add spacing between buttons
        ) {
            // Surveillance Button
            Button(
                onClick = { onAction(LandingAction.StartNewSurveillanceSession) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp), // Larger height for the button
                shape = MaterialTheme.shapes.medium, // Rounded rectangle shape
            ) {
                Text(
                    text = "New Surveillance Session",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Non-Surveillance Button
            Button(
                onClick = { onAction(LandingAction.StartNewNonSurveillanceSession) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp), // Larger height for the button
                shape = MaterialTheme.shapes.medium, // Rounded rectangle shape
            ) {
                Text(
                    text = "New Non-Surveillance Session",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Incomplete Sessions Button with Badge
            OutlinedButton(
                onClick = { onAction(LandingAction.ViewIncompleteSessions) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp), // Larger height for the button
                shape = MaterialTheme.shapes.medium, // Rounded rectangle shape
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary // Text color matches border
                ),
                border = BorderStroke(
                    2.dp, MaterialTheme.colorScheme.secondary
                ) // Custom border
            ) {
                Text(
                    text = "View Incomplete Sessions",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp)) // Space between text and badge
                if (state.incompleteSessions.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${state.incompleteSessions.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onError,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Complete Sessions Button
            OutlinedButton(
                onClick = { onAction(LandingAction.ViewCompleteSessions) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp), // Larger height for the button
                shape = MaterialTheme.shapes.medium, // Rounded rectangle shape
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary // Text color matches border
                ),
                border = BorderStroke(
                    2.dp, MaterialTheme.colorScheme.secondary
                ) // Custom border
            ) {
                Text(
                    text = "View Complete Sessions",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Footer Section
        Text(
            text = "Version: ${state.versionName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@PreviewLightDark
@Composable
fun LandingScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            LandingScreen(
                state = LandingState(), onAction = { }, modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
