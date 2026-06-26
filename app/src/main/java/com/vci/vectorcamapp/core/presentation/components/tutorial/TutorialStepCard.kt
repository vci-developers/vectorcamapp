package com.vci.vectorcamapp.core.presentation.components.tutorial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.tutorial.TutorialStep
import com.vci.vectorcamapp.core.presentation.tutorial.LocalTutorialManager
import com.vci.vectorcamapp.ui.extensions.colors

private data class TutorialContent(val title: String, val description: String)

private val tutorialContentMap = mapOf(
    TutorialStep.REGISTRATION_FORM to TutorialContent(
        title = "Step 1 — Register Your Device",
        description = "Select your Program, then enter your Collector Name, Title, and the date you were last trained. Tap Confirm to complete registration."
    ),
    TutorialStep.NEW_SURVEILLANCE_SESSION to TutorialContent(
        title = "Step 2 — Start a New Session",
        description = "Tap the highlighted New Surveillance Session tile to begin a household visit and start capturing mosquito images."
    ),
    TutorialStep.INTAKE_FORM to TutorialContent(
        title = "Step 3 — Fill In Session Details",
        description = "Complete all required fields — location, household information, and collection details. Tap Submit when everything looks correct."
    ),
    TutorialStep.CAPTURE_AND_SAVE to TutorialContent(
        title = "Step 4 — Capture & Save Images",
        description = "A Specimen ID appears automatically. Tap Capture to photograph the specimen. Review the image, confirm or edit the Specimen ID, then tap Save to add it to the session."
    ),
    TutorialStep.IN_PROGRESS_SESSIONS to TutorialContent(
        title = "Step 5 — Sessions In Progress",
        description = "Tap the highlighted tile to view and resume any sessions you have saved but not yet submitted."
    ),
    TutorialStep.COMPLETE_SESSIONS to TutorialContent(
        title = "Step 6 — Complete Sessions",
        description = "Tap the highlighted tile to review all fully completed and uploaded sessions. This is your session history."
    )
)

@Composable
fun TutorialStepCard(modifier: Modifier = Modifier) {
    val tutorialManager = LocalTutorialManager.current
    val currentStep by tutorialManager.currentStep.collectAsState()

    val content = tutorialContentMap[currentStep] ?: return

    val progress = currentStep.stepIndex.toFloat() / TutorialStep.TOTAL_STEPS.toFloat()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    tint = MaterialTheme.colors.icon,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${currentStep.stepIndex} / ${TutorialStep.TOTAL_STEPS}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colors.textSecondary
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.icon,
                trackColor = MaterialTheme.colors.icon.copy(alpha = 0.2f),
                strokeCap = StrokeCap.Round
            )

            Text(
                text = content.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colors.textSecondary
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = { tutorialManager.skipTutorial() }) {
                    Text(
                        text = "Skip Tutorial",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { tutorialManager.advanceStep() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colors.icon
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (currentStep.isLast) "Done" else "Next  →",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.buttonText
                    )
                }
            }
        }
    }
}
