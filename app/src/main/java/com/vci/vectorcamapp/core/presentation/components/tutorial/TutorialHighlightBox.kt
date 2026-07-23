package com.vci.vectorcamapp.core.presentation.components.tutorial

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.core.domain.tutorial.TutorialStep
import com.vci.vectorcamapp.core.presentation.tutorial.LocalSpotlightBounds
import com.vci.vectorcamapp.core.presentation.tutorial.LocalTutorialManager

private val TutorialHighlightColor = Color(0xFF4CAF50)

private val spotlightSteps = setOf(
    TutorialStep.NEW_SURVEILLANCE_SESSION,
    TutorialStep.IN_PROGRESS_SESSIONS,
    TutorialStep.COMPLETE_SESSIONS
)

@Composable
fun TutorialHighlightBox(
    step: TutorialStep,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    content: @Composable () -> Unit
) {
    val tutorialManager = LocalTutorialManager.current
    val currentStep by tutorialManager.currentStep.collectAsState()
    val isActive = currentStep == step
    val isSpotlight = step in spotlightSteps

    if (isActive && isSpotlight) {
        val spotlightBounds = LocalSpotlightBounds.current
        Box(
            modifier = modifier.onGloballyPositioned { coords ->
                spotlightBounds.value = coords.boundsInWindow()
            }
        ) {
            content()
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tutorial_pulse_$step")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tutorial_alpha_$step"
    )
    val borderWidth by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tutorial_border_$step"
    )

    Box(
        modifier = modifier.then(
            if (isActive) {
                Modifier
                    .border(
                        width = borderWidth.dp,
                        color = TutorialHighlightColor.copy(alpha = alpha),
                        shape = RoundedCornerShape(cornerRadius.dp)
                    )
                    .padding(2.dp)
            } else {
                Modifier
            }
        )
    ) {
        content()
    }
}
