package com.vci.vectorcamapp.complete_session.list.presentation.components

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.Site
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.domain.model.enums.UploadStatus
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.components.tile.ActionTile
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CompleteSessionListTile(
    session: Session,
    site: Site,
    specimens: List<SpecimenWithSpecimenImagesAndInferenceResults>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateTimeFormatter =
        remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    val totalImages = specimens.sumOf { it.specimenImagesAndInferenceResults.size }
    val uploadedImages = specimens.sumOf { specimen ->
        specimen.specimenImagesAndInferenceResults.count { (specimenImage, _) ->
            specimenImage.metadataUploadStatus == UploadStatus.COMPLETED &&
                    specimenImage.imageUploadStatus == UploadStatus.COMPLETED
        }
    }
    val uploadProgress = if (totalImages > 0) uploadedImages.toFloat() / totalImages else 0f

    session.completedAt?.let { completedAt ->
        ActionTile(
            onClick = onClick,
            modifier = modifier,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingLarge)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
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
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
                            )
                        }
                    }
                    InfoPill(text = "Session Type: ${session.type}", color = MaterialTheme.colors.info)
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
                        text = "Sentinel Site: ${site.sentinelSite}",
                    )

                    CompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_house),
                        iconDescription = "House",
                        text = "House Number: ${session.houseNumber}",
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colors.divider,
                    thickness = MaterialTheme.dimensions.dividerThickness
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
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

                    Spacer(modifier = Modifier.height(MaterialTheme.dimensions.spacingSmall))

                    if (totalImages > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Upload Progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colors.textSecondary
                            )
                            Text(
                                text = "$uploadedImages / $totalImages images",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colors.textSecondary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MaterialTheme.dimensions.componentHeightExtraExtraExtraSmall),
                            color = if (uploadProgress == 1f) MaterialTheme.colors.primary else MaterialTheme.colors.secondary,
                            trackColor = MaterialTheme.colors.divider,
                            strokeCap = StrokeCap.Butt,
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                    }
                }
            }
        }
    }
}

