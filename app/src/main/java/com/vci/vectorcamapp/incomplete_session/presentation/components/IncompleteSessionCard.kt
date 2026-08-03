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
import androidx.compose.ui.res.stringResource
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
                            text = stringResource(R.string.incomplete_session_title_card, titleFormatter.format(sessionAndSite.session.createdAt)),
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
                                contentDescription = stringResource(R.string.incomplete_session_content_description_resume),
                                tint = MaterialTheme.colors.icon,
                                modifier = Modifier.size(MaterialTheme.dimensions.iconSizeMedium)
                                .testTag(IncompleteSessionTestTags.CARD_RESUME_ICON)
                            )
                        }
                    }

                    InfoPill(text = stringResource(R.string.incomplete_session_label_session_type, sessionAndSite.session.type.displayText(context)), color = MaterialTheme.colors.info, modifier = Modifier.testTag(IncompleteSessionTestTags.CARD_TYPE_PILL))
                }

                Column(
                    modifier = Modifier.padding(vertical = MaterialTheme.dimensions.paddingSmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium)
                ) {
                    IncompleteSessionListDetailRow(
                        iconPainter = painterResource(R.drawable.ic_person),
                        iconDescription = stringResource(R.string.incomplete_session_content_description_person),
                        text = stringResource(R.string.incomplete_session_label_collector, sessionAndSite.session.collectorName, sessionAndSite.session.collectorTitle),
                    )

                    sessionAndSite.site.district?.let { district ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_pin),
                            iconDescription = stringResource(R.string.incomplete_session_content_description_pin),
                            text = stringResource(R.string.incomplete_session_label_district, district),
                        )
                    }

                    sessionAndSite.site.subCounty?.let { subCounty ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_map),
                            iconDescription = stringResource(R.string.incomplete_session_content_description_map),
                            text = stringResource(R.string.incomplete_session_label_sub_county, subCounty),
                        )
                    }

                    sessionAndSite.site.parish?.let { parish ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_navigation),
                            iconDescription = stringResource(R.string.incomplete_session_content_description_navigation),
                            text = stringResource(R.string.incomplete_session_label_parish, parish),
                        )
                    }

                    sessionAndSite.site.villageName?.let { villageName ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_clipboard),
                            iconDescription = stringResource(R.string.incomplete_session_content_description_clipboard),
                            text = stringResource(R.string.incomplete_session_label_village_name, villageName),
                        )
                    }

                    sessionAndSite.site.houseNumber?.let { houseNumber ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_house),
                            iconDescription = stringResource(R.string.incomplete_session_content_description_house),
                            text = stringResource(R.string.incomplete_session_label_house_number, houseNumber),
                        )
                    }

                    sessionAndSite.site.healthCenter?.let { healthCenter ->
                        IncompleteSessionListDetailRow(
                            iconPainter = painterResource(R.drawable.ic_hospital),
                            iconDescription = stringResource(R.string.incomplete_session_content_description_hospital),
                            text = stringResource(R.string.incomplete_session_label_health_center, healthCenter),
                        )
                    }

                    sessionAndSite.site.locationHierarchy
                        ?.filterValues { it.isNotBlank() }
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { locationHierarchy ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pin),
                                    contentDescription = stringResource(R.string.incomplete_session_content_description_location),
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
                                            text = stringResource(R.string.incomplete_session_label_detail, key, value),
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
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingSmall)
                ) {
                    Text(
                        text = stringResource(R.string.incomplete_session_label_created_at, detailFormatter.format(sessionAndSite.session.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colors.textSecondary,
                        modifier = Modifier.testTag(IncompleteSessionTestTags.CARD_CREATED_TEXT)
                    )
                }
            }
        }
    }
}
