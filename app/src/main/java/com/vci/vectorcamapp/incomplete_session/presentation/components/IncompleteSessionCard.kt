package com.vci.vectorcamapp.incomplete_session.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.components.tile.ActionTile
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun IncompleteSessionCard(
    session: Session,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val deleteWidthPx = with(density) { (MaterialTheme.dimensions.spacingExtraLarge * 2).toPx() }
    val deleteWidthDp = with(density) { deleteWidthPx.toDp() }
    val maxOffset = -deleteWidthPx
    val shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium)

    val titleFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val detailFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    Box(
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.dimensions.paddingMedium,
                vertical = MaterialTheme.dimensions.paddingSmall
            )
    ) {
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

        // ActionTile with swipe gestures applied via modifier
        ActionTile(
            onClick = onClick,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.dimensions.paddingLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Incomplete Session on ${titleFormatter.format(session.createdAt)}",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(Modifier.height(MaterialTheme.dimensions.spacingSmall))

                    InfoPill(text = "Session Type: ${session.type}", color = MaterialTheme.colors.info)

                    Spacer(Modifier.height(MaterialTheme.dimensions.spacingSmall))

                    Text(
                        text = "Created: ${detailFormatter.format(session.createdAt)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Last Updated: placeholder",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.width(MaterialTheme.dimensions.spacingSmall))

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "Resume",
                    modifier = Modifier
                        .size(MaterialTheme.dimensions.iconSizeLarge + MaterialTheme.dimensions.paddingExtraSmall)
                        .background(MaterialTheme.colors.iconBackground, CircleShape)
                        .padding(MaterialTheme.dimensions.paddingExtraSmall),
                    tint = MaterialTheme.colors.icon
                )
            }
        }
    }
}
