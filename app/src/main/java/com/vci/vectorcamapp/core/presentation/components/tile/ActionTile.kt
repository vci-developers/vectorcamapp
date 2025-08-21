package com.vci.vectorcamapp.core.presentation.components.tile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.customShadow
import com.vci.vectorcamapp.ui.extensions.dimensions
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ActionTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium)
    val cardModifier = modifier
        .padding(
            horizontal = MaterialTheme.dimensions.paddingMedium,
            vertical = MaterialTheme.dimensions.paddingSmall
        )
        .customShadow(
            color = MaterialTheme.colors.cardGlow.copy(alpha = 0.2f),
            offsetX = MaterialTheme.dimensions.shadowOffsetSmall,
            offsetY = MaterialTheme.dimensions.shadowOffsetSmall,
            cornerRadius = MaterialTheme.dimensions.cornerRadiusMedium,
            spread = MaterialTheme.dimensions.shadowSpreadSmall,
            blurRadius = MaterialTheme.dimensions.shadowBlurSmall,
        )
        .fillMaxWidth()
        .clip(shape)

    if (onDelete != null) {
        var offsetX by remember { mutableFloatStateOf(0f) }
        val density = LocalDensity.current
        val deleteWidthPx = with(density) { MaterialTheme.dimensions.spacingExtraExtraLarge.toPx() }
        val deleteWidthDp = with(density) { deleteWidthPx.toDp() }
        val maxOffset = -deleteWidthPx

        Box(modifier = cardModifier) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.error, shape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(deleteWidthDp)
                        .align(Alignment.CenterEnd)
                        .clickable {
                            offsetX = 0f
                            onDelete()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colors.buttonText,
                        modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), 0) }
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                offsetX = (offsetX + dragAmount).coerceIn(maxOffset, 0f)
                            },
                            onDragEnd = {
                                offsetX = if (abs(offsetX) > deleteWidthPx / 2) maxOffset else 0f
                            }
                        )
                    }
            ) {
                Card(
                    onClick = onClick,
                    border = CardDefaults.outlinedCardBorder(),
                    shape = shape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colors.cardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    content()
                }
            }
        }
    } else {
        Card(
            onClick = onClick,
            border = CardDefaults.outlinedCardBorder(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colors.cardBackground),
            modifier = cardModifier
        ) {
            content()
        }
    }
}
