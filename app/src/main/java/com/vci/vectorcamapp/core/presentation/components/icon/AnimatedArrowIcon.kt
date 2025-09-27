package com.vci.vectorcamapp.core.presentation.components.icon

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.empty.EmptySpace
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun AnimatedArrowIcon(
    isLeft: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    amplitude: Dp = MaterialTheme.dimensions.spacingSmall,
    durationMillis: Int = 800
) {
    if (!enabled) {
        EmptySpace(
            width = MaterialTheme.dimensions.iconSizeLarge,
            height = MaterialTheme.dimensions.iconSizeLarge
        )
        return
    }

    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "arrow_live")
    val arrowOffsetX by transition.animateFloat(
        initialValue = with(density) { amplitude.toPx() },
        targetValue = with(density) { -amplitude.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset_live"
    )

    Icon(
        painter = painterResource(
            id = if (isLeft) R.drawable.ic_arrow_left else R.drawable.ic_arrow_right
        ),
        contentDescription = if (isLeft) "Previous Icon" else "Next Icon",
        tint = MaterialTheme.colors.icon,
        modifier = modifier
            .offset { IntOffset((if (isLeft) -arrowOffsetX else arrowOffsetX).toInt(), 0) }
            .size(MaterialTheme.dimensions.iconSizeLarge)
            .clickable(onClick = onClick)
    )
}
