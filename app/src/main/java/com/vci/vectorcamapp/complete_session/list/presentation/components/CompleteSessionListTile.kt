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
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.domain.model.helpers.SessionUploadProgress
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.components.tile.ActionTile
import com.vci.vectorcamapp.core.presentation.extensions.displayText
import com.vci.vectorcamapp.ui.extensions.colors
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

    val progressColor = when {
        sessionMetadataUploaded && sessionUploadProgress.uploadedMetadataCount == sessionUploadProgress.totalCount && sessionUploadProgress.uploadedImageCount == sessionUploadProgress.totalCount -> MaterialTheme.colors.primary
        sessionUploadProgress.isUploading -> MaterialTheme.colors.warning
        else -> MaterialTheme.colors.error
    }

    session.completedAt?.let { completedAt ->
        ActionTile(
            onClick = onClick,
            hue = progressColor,
            modifier = modifier
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
                            text = stringResource(R.string.complete_session_title_card, dateFormatter.format(completedAt)),
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
                                contentDescription = stringResource(R.string.complete_session_content_description_arrow),
                                tint = MaterialTheme.colors.icon,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeLarge)
                            )
                        }
                    }
                    InfoPill(
                        text = stringResource(R.string.complete_session_label_session_type, session.type.displayText(context)),
                        color = MaterialTheme.colors.info
                    )
                }

                Column(
                    modifier = Modifier.padding(vertical = MaterialTheme.dimensions.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                ) {
                    CompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_person),
                        iconDescription = stringResource(R.string.complete_session_content_description_person),
                        text = stringResource(R.string.complete_session_label_collector, session.collectorName, session.collectorTitle),
                    )

                    site.district?.let { district ->
                        CompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_pin),
                            iconDescription = stringResource(R.string.complete_session_content_description_pin),
                            text = stringResource(R.string.complete_session_label_district, district),
                        )
                    }

                    site.subCounty?.let { subCounty ->
                        CompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_map),
                            iconDescription = stringResource(R.string.complete_session_content_description_map),
                            text = stringResource(R.string.complete_session_label_sub_county, subCounty),
                        )
                    }

                    site.parish?.let { parish ->
                        CompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_navigation),
                            iconDescription = stringResource(R.string.complete_session_content_description_navigation),
                            text = stringResource(R.string.complete_session_label_parish, parish),
                        )
                    }

                    site.villageName?.let { villageName ->
                        CompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_clipboard),
                            iconDescription = stringResource(R.string.complete_session_content_description_clipboard),
                            text = stringResource(R.string.complete_session_label_village_name, villageName),
                        )
                    }

                    site.houseNumber?.let { houseNumber ->
                        CompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_house),
                            iconDescription = stringResource(R.string.complete_session_content_description_house),
                            text = stringResource(R.string.complete_session_label_house_number, houseNumber),
                        )
                    }

                    site.healthCenter?.let { healthCenter ->
                        CompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_hospital),
                            iconDescription = stringResource(R.string.complete_session_content_description_hospital),
                            text = stringResource(R.string.complete_session_label_health_center, healthCenter),
                        )
                    }

                    site.locationHierarchy
                        ?.filterValues { it.isNotBlank() }
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { locationHierarchy ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pin),
                                    contentDescription = stringResource(R.string.complete_session_content_description_location),
                                    tint = MaterialTheme.colors.icon,
                                    modifier = Modifier.size(MaterialTheme.dimensions.iconSizeSmall)
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(
                                        MaterialTheme.dimensions.spacingExtraSmall
                                    )
                                ) {
                                    locationHierarchy.forEach { (key, value) ->
                                        Text(
                                            text = stringResource(R.string.complete_session_label_detail, key, value),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colors.textPrimary
                                        )
                                    }
                                }
                            }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colors.divider,
                    thickness = MaterialTheme.dimensions.dividerThicknessThick
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingExtraSmall)
                ) {
                    Text(
                        text = stringResource(R.string.complete_session_label_created_at, dateTimeFormatter.format(session.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textSecondary
                    )

                    Text(
                        text = stringResource(R.string.complete_session_label_completed_at, dateTimeFormatter.format(session.completedAt)),
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
                                text = stringResource(R.string.complete_session_label_upload_progress),
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
                                    contentDescription = stringResource(R.string.complete_session_content_description_uploading),
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
                                sessionMetadataUploaded && sessionUploadProgress.uploadedMetadataCount == sessionUploadProgress.totalCount && sessionUploadProgress.uploadedImageCount == sessionUploadProgress.totalCount -> stringResource(R.string.complete_session_status_completed)
                                !sessionMetadataUploaded -> stringResource(R.string.complete_session_status_not_started)
                                sessionUploadProgress.uploadedMetadataCount != sessionUploadProgress.totalCount -> stringResource(R.string.complete_session_status_metadata_progress, sessionUploadProgress.uploadedMetadataCount, sessionUploadProgress.totalCount)
                                else -> stringResource(R.string.complete_session_status_image_progress, sessionUploadProgress.uploadedImageCount, sessionUploadProgress.totalCount)
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
