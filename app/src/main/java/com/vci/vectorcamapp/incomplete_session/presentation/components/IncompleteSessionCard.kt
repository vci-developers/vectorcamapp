package com.vci.vectorcamapp.incomplete_session.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.composites.SessionAndSite
import com.vci.vectorcamapp.core.presentation.components.gestures.SwipeToReveal
import com.vci.vectorcamapp.core.presentation.components.pill.InfoPill
import com.vci.vectorcamapp.core.presentation.components.tile.ActionTile
import com.vci.vectorcamapp.incomplete_session.presentation.util.IncompleteSessionTestTags
import com.vci.vectorcamapp.core.presentation.extensions.displayText
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun IncompleteSessionCard(
    sessionAndSite: SessionAndSite,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                modifier = Modifier.padding(MaterialTheme.dimensions.paddingLarge)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Session in Progress from\n${titleFormatter.format(sessionAndSite.session.createdAt)}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colors.textPrimary,
                            modifier = Modifier.testTag(IncompleteSessionTestTags.CARD_TITLE)
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
                                contentDescription = "Resume",
                                tint = MaterialTheme.colors.icon,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
                                .testTag(IncompleteSessionTestTags.CARD_RESUME_ICON)
                            )
                        }
                    }

                    InfoPill(text = "Session Type: ${sessionAndSite.session.type.displayText(context)}", color = MaterialTheme.colors.info, modifier = Modifier.testTag(IncompleteSessionTestTags.CARD_TYPE_PILL))
                }

                Column(
                    modifier = Modifier.padding(vertical = MaterialTheme.dimensions.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                ) {
                    IncompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_person),
                        iconDescription = "Person",
                        text = "Collector: ${sessionAndSite.session.collectorName}, ${sessionAndSite.session.collectorTitle}",
                    )

                    sessionAndSite.site.district?.let { district ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_pin),
                            iconDescription = "Pin",
                            text = "District: $district",
                        )
                    }

                    sessionAndSite.site.subCounty?.let { subCounty ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_map),
                            iconDescription = "Map",
                            text = "Sub-County: $subCounty",
                        )
                    }

                    sessionAndSite.site.parish?.let { parish ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_navigation),
                            iconDescription = "Navigation",
                            text = "Parish: $parish",
                        )
                    }

                    sessionAndSite.site.villageName?.let { villageName ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_clipboard),
                            iconDescription = "Clipboard",
                            text = "Village Name: $villageName",
                        )
                    }

                    sessionAndSite.site.houseNumber?.let { houseNumber ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_house),
                            iconDescription = "House",
                            text = "House Number: $houseNumber",
                        )
                    }

                    sessionAndSite.site.healthCenter?.let { healthCenter ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_hospital),
                            iconDescription = "Hospital",
                            text = "Nearest Health Center: $healthCenter",
                        )
                    }

                    sessionAndSite.site.locationHierarchy?.let { locationHierarchy ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_pin),
                            iconDescription = "Location",
                            text = "Location: $locationHierarchy",
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colors.divider,
                    thickness = MaterialTheme.dimensions.dividerThicknessThick
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                ) {
                    Text(
                        text = "Created At: ${detailFormatter.format(sessionAndSite.session.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textSecondary,
                        modifier = Modifier.testTag(IncompleteSessionTestTags.CARD_CREATED_TEXT)
                    )
                }
            }
        }
    }
}
