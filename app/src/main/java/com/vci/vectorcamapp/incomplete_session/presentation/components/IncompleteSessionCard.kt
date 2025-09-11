package com.vci.vectorcamapp.incomplete_session.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.components.gestures.SwipeToReveal
import com.vci.vectorcamapp.core.presentation.components.tile.ActionTile
import com.vci.vectorcamapp.incomplete_session.presentation.util.IncompleteSessionTestTags
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun IncompleteSessionCard(
    session: Session,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val detailFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    SwipeToReveal(
        backgroundContent = {
            IncompleteSessionDeleteBackground(
                onDelete = onDelete,
                deleteWidth = MaterialTheme.dimensions.spacingExtraExtraExtraLarge
            )
        },
        revealWidth = MaterialTheme.dimensions.spacingExtraExtraExtraLarge,
        modifier = modifier
    ) {
        ActionTile(onClick = onClick) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.dimensions.paddingLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Incomplete Session on ${titleFormatter.format(session.createdAt)}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.testTag(IncompleteSessionTestTags.CARD_TITLE)
                    )

                    Spacer(Modifier.height(MaterialTheme.dimensions.spacingSmall))

                    InfoPill(text = "Session Type: ${session.type}", color = MaterialTheme.colors.info, modifier = Modifier.testTag(IncompleteSessionTestTags.CARD_TYPE_PILL))

                    Spacer(Modifier.height(MaterialTheme.dimensions.spacingSmall))

                    Text(
                        text = "Created: ${detailFormatter.format(session.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag(IncompleteSessionTestTags.CARD_CREATED_TEXT)
                    )
                    Text(
                        text = "Last Updated: placeholder",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag(IncompleteSessionTestTags.CARD_UPDATED_TEXT)
                    )
                }

                Spacer(Modifier.width(MaterialTheme.dimensions.spacingSmall))

                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "Resume",
                    modifier = Modifier
                        .size(MaterialTheme.dimensions.iconSizeLarge + MaterialTheme.dimensions.paddingExtraSmall)
                        .background(MaterialTheme.colors.iconBackground, CircleShape)
                        .padding(MaterialTheme.dimensions.paddingExtraSmall)
                        .testTag(IncompleteSessionTestTags.CARD_RESUME_ICON),
                    tint = MaterialTheme.colors.icon
                )
            }
        }
    }
}
