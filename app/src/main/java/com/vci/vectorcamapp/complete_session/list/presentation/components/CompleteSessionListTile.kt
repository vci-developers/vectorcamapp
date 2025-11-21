package com.vci.vectorcamapp.complete_session.list.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.helpers.SessionUploadProgress
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.components.tile.ActionTile
import com.vci.vectorcamapp.core.presentation.extensions.displayText
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.customShadow
import com.vci.vectorcamapp.ui.extensions.dimensions
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CompleteSessionListTile(
    sessionAndSite: SessionAndSite,
    sessionUploadProgress: SessionUploadProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val UPLOAD_ICON_ANIMATION_DURATION_MS = 1000
    val UPLOAD_ICON_MIN_ALPHA = 0.3f
    val UPLOAD_ICON_MAX_ALPHA = 1f

    val PROGRESS_BAR_LOW_OPACITY = 0.3f

    val session = sessionAndSite.session
    val site = sessionAndSite.site
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateTimeFormatter =
        remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    val sessionMetadataUploaded = session.submittedAt != null

    val progressColor =
        if (sessionMetadataUploaded && sessionUploadProgress.uploadedMetadataCount == sessionUploadProgress.totalCount && sessionUploadProgress.uploadedImageCount == sessionUploadProgress.totalCount) MaterialTheme.colors.primary
        else if (sessionUploadProgress.isUploading) MaterialTheme.colors.warning
        else MaterialTheme.colors.error

    session.completedAt?.let { completedAt ->
        ActionTile(
            onClick = onClick,
            hue = progressColor,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingLarge)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Completed Session on\n${dateFormatter.format(completedAt)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colors.textPrimary
                        )

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
                                contentDescription = "Arrow Right",
                                tint = MaterialTheme.colors.icon,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
                            )
                        }
                    }
                    InfoPill(
                        text = "Session Type: ${session.type.displayText(context)}",
                        color = MaterialTheme.colors.info
                    )
                }

                Column(
                    modifier = Modifier.padding(vertical = MaterialTheme.dimensions.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                ) {
                    CompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_person),
                        iconDescription = "Person",
                        text = "Collector: ${session.collectorName}, ${session.collectorTitle}",
                    )

                    CompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_pin),
                        iconDescription = "Pin",
                        text = "District: ${site.district}",
                    )

                    CompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_map),
                        iconDescription = "Map",
                        text = "Sub-County: ${site.subCounty}",
                    )

                    CompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_navigation),
                        iconDescription = "Navigation",
                        text = "Parish: ${site.parish}",
                    )

                    CompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_clipboard),
                        iconDescription = "Clipboard",
                        text = "Village Name: ${site.villageName}",
                    )

                    CompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_house),
                        iconDescription = "House",
                        text = "House Number: ${site.houseNumber}",
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colors.divider,
                    thickness = MaterialTheme.dimensions.dividerThickness
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall)
                ) {
                    Text(
                        text = "Created At: ${dateTimeFormatter.format(session.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textSecondary
                    )

                    Text(
                        text = "Completed At: ${dateTimeFormatter.format(session.completedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingExtraSmall))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraExtraSmall)
                        ) {
                            Text(
                                text = "Upload Progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colors.textSecondary
                            )
                            if (sessionUploadProgress.isUploading) {
                                val infiniteTransition =
                                    rememberInfiniteTransition(label = "upload_pulse")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = UPLOAD_ICON_MIN_ALPHA,
                                    targetValue = UPLOAD_ICON_MAX_ALPHA,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = UPLOAD_ICON_ANIMATION_DURATION_MS,
                                            easing = LinearEasing
                                        ),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "alpha"
                                )

                                Icon(
                                    painter = painterResource(R.drawable.ic_cloud_upload),
                                    contentDescription = "Currently uploading",
                                    tint = MaterialTheme.colors.textSecondary,
                                    modifier = Modifier
                                        .padding(horizontal = MaterialTheme.dimensions.paddingExtraSmall)
                                        .size(MaterialTheme.dimensions.iconSizeExtraSmall)
                                        .alpha(alpha)
                                )
                            }
                        }
                        Text(
                            text = when {
                                sessionMetadataUploaded && sessionUploadProgress.uploadedMetadataCount == sessionUploadProgress.totalCount && sessionUploadProgress.uploadedImageCount == sessionUploadProgress.totalCount -> "Completed"
                                !sessionMetadataUploaded -> "Not started"
                                sessionUploadProgress.uploadedMetadataCount != sessionUploadProgress.totalCount -> "${sessionUploadProgress.uploadedMetadataCount} / ${sessionUploadProgress.totalCount} metadata"
                                else -> "${sessionUploadProgress.uploadedImageCount} / ${sessionUploadProgress.totalCount} images"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaterialTheme.dimensions.componentHeightExtraExtraExtraSmall)
                            .background(
                                MaterialTheme.colors.divider,
                                RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)
                            )
                    ) {
                        val metadataProgress = when {
                            !sessionMetadataUploaded -> 0f
                            sessionUploadProgress.totalCount == 0 -> 1f
                            else -> sessionUploadProgress.uploadedMetadataCount.toFloat() / sessionUploadProgress.totalCount.toFloat()
                        }
                        val imageProgress = when {
                            !sessionMetadataUploaded -> 0f
                            sessionUploadProgress.totalCount == 0 -> 1f
                            else -> sessionUploadProgress.uploadedImageCount.toFloat() / sessionUploadProgress.totalCount.toFloat()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(metadataProgress)
                                .height(MaterialTheme.dimensions.componentHeightExtraExtraExtraSmall)
                                .background(
                                    progressColor.copy(PROGRESS_BAR_LOW_OPACITY),
                                    RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(imageProgress)
                                .height(MaterialTheme.dimensions.componentHeightExtraExtraExtraSmall)
                                .background(
                                    progressColor,
                                    RoundedCornerShape(MaterialTheme.dimensions.cornerRadiusSmall)
                                )
                        )
                    }
                }
            }
        }
    }
}
