package com.vci.vectorcamapp.incomplete_session.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.ui.extensions.customShadow
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.LocalColors
import com.vci.vectorcamapp.ui.theme.LocalDimensions
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun IncompleteSessionCard(
    session: Session,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current

    val titleFormatter  = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val detailFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .customShadow(
                color        = colors.cardGlow.copy(alpha = 0.25f),
                offsetY      = MaterialTheme.dimensions.shadowOffsetYSmall,
                blurRadius   = MaterialTheme.dimensions.shadowBlurMedium,
                cornerRadius = MaterialTheme.dimensions.cornerRadiusMedium
            )
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusMedium),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = colors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimensions.paddingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text  = "Incomplete Session on ${titleFormatter.format(session.createdAt)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Box(
                    modifier = Modifier
                        .padding(top = MaterialTheme.dimensions.spacingSmall)
                        .background(
                            color = colors.pillBackground,
                            shape = RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)
                        )
                        .padding(horizontal = MaterialTheme.dimensions.paddingMedium, vertical = 4.dp)
                ) {
                    Text(
                        text  = "Session ID: ${session.localId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.pillText
                    )
                }

                Spacer(Modifier.height(MaterialTheme.dimensions.spacingMedium))
                Text(
                    text  = "Created: ${detailFormatter.format(session.createdAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text  = "Last Updated: placeholder",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.width(MaterialTheme.dimensions.spacingSmall))

            Icon(
                imageVector   = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Resume",
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge + 6.dp)
                    .background(colors.iconBackground, CircleShape)
                    .padding(12.dp),
                tint = colors.icon
            )
        }
    }
}
