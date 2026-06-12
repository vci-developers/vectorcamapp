package com.vci.vectorcamapp.complete_session.details.presentation.components.specimens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.presentation.search.SearchHelpTooltipContent
import com.vci.vectorcamapp.core.presentation.search.SearchTextField
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.screenWidthFraction
import java.util.UUID

/* TODO: CLEANUP */
@Composable
fun CompleteSessionSpecimens(
    specimensWithImagesAndInferenceResults: List<SpecimenWithSpecimenImagesAndInferenceResults>,
    sessionUnitIdBySpecimenId: Map<String, UUID?>,
    bucketNameBySessionUnitId: Map<UUID, String>,
    searchQuery: String,
    onUpdateSearchQuery: (String) -> Unit,
    isSearchTooltipVisible: Boolean,
    onShowSearchTooltip: () -> Unit,
    onDismissSearchTooltip: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (specimensWithImagesAndInferenceResults.isEmpty() && searchQuery.isBlank()) {
        Text(
            stringResource(R.string.complete_session_body_no_specimens),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)
        )
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SearchTextField(
                searchQuery = searchQuery,
                onSearchQueryChange = onUpdateSearchQuery,
                placeholder = stringResource(R.string.complete_session_placeholder_search_specimens),
                modifier = Modifier.padding(
                    start = MaterialTheme.dimensions.spacingMedium,
                    end = MaterialTheme.dimensions.spacingMedium,
                    top = MaterialTheme.dimensions.spacingSmall
                ),
                isTooltipVisible = isSearchTooltipVisible,
                onShowSearchTooltip = onShowSearchTooltip,
                onDismissSearchTooltip = onDismissSearchTooltip,
            ) {
                SearchHelpTooltipContent()
            }

            if (specimensWithImagesAndInferenceResults.isEmpty()) {
                Text(
                    text = stringResource(R.string.complete_session_body_no_matching_specimens),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(items = specimensWithImagesAndInferenceResults.asReversed()) { specimenWithSpecimenImagesAndInferenceResults ->
                        val specimen = specimenWithSpecimenImagesAndInferenceResults.specimen
                        val imageList =
                            specimenWithSpecimenImagesAndInferenceResults.specimenImagesAndInferenceResults
                        val totalImages = imageList.size

                        val sessionUnitId = sessionUnitIdBySpecimenId[specimen.id]
                        val batchName = sessionUnitId?.let { bucketNameBySessionUnitId[it] }

                        imageList.mapIndexed { index, (specimenImage, _) ->
                            CompleteSessionSpecimensTile(
                                specimen = specimen,
                                specimenImage = specimenImage,
                                badgeText = stringResource(R.string.complete_session_label_image_badge, index + 1, totalImages),
                                batchName = batchName,
                                modifier = Modifier.width(
                                    screenWidthFraction(0.9f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
