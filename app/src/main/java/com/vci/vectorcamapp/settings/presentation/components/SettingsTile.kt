package com.vci.vectorcamapp.settings.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.tile.ActionTile
import com.vci.vectorcamapp.core.presentation.components.tile.InfoTile
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun SettingsTile(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) -180f else 0f,
        label = "chevron_rotation"
    )

    val tileContent: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
            modifier = Modifier.padding(MaterialTheme.dimensions.paddingLarge)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colors.textPrimary
                )

                if (onClick != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(MaterialTheme.dimensions.componentHeightSmall)
                            .background(
                                color = MaterialTheme.colors.iconBackground,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = "Navigate",
                            tint = MaterialTheme.colors.icon,
                            modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
                        )
                    }
                } else if (content != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(MaterialTheme.dimensions.componentHeightSmall)
                            .background(
                                color = MaterialTheme.colors.iconBackground,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_down),
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colors.icon,
                            modifier = Modifier
                                .size(MaterialTheme.dimensions.iconSizeMedium)
                                .rotate(chevronRotation)
                        )
                    }
                }
            }

            if (onClick == null && content != null) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        content()
                    }
                }
            }
        }
    }

    val handleClick =
        onClick
            ?: if (content != null) {
                { isExpanded = !isExpanded }
            } else {
                null
            }

    if (handleClick != null) {
        ActionTile(
            onClick = handleClick,
            modifier = modifier,
            content = tileContent
        )
    } else {
        InfoTile(
            modifier = modifier,
            content = tileContent
        )
    }
}
