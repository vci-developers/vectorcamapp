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
import androidx.compose.ui.text.style.TextAlign
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsAction
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.presentation.search.SearchHelpTooltipContent
import com.vci.vectorcamapp.core.presentation.search.SearchTextField
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.screenWidthFraction

@Composable
fun CompleteSessionSpecimens(
    specimensWithImagesAndInferenceResults: List<SpecimenWithSpecimenImagesAndInferenceResults>,
    searchQuery: String,
    onUpdateSearchQuery: (String) -> Unit,
    isSearchTooltipVisible: Boolean,
    onAction: (CompleteSessionDetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (specimensWithImagesAndInferenceResults.isEmpty() && searchQuery.isBlank()) {
        Text(
            "No specimens were captured during this session.",
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
                onSearchQueryChange = { newSearchQueryText ->
                    onUpdateSearchQuery(newSearchQueryText)
                },
                placeholder = "Search by specimen ID, species, etc.",
                modifier = Modifier.padding(
                    start = MaterialTheme.dimensions.spacingMedium,
                    end = MaterialTheme.dimensions.spacingMedium,
                    bottom = MaterialTheme.dimensions.spacingSmall
                ),
                onTooltipPrimaryAction = { onAction(CompleteSessionDetailsAction.ShowSearchTooltipDialog) },
                onTooltipDismiss = { onAction(CompleteSessionDetailsAction.HideSearchTooltipDialog) },
                tooltipButtonText = "Tap to learn more about search and filter logic"
            ) {
                SearchHelpTooltipContent()
            }

            if (specimensWithImagesAndInferenceResults.isEmpty()) {
                Text(
                    text = "No matching specimens found.",
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
                        imageList.mapIndexed { index, (specimenImage, _) ->
                            CompleteSessionSpecimensTile(
                                specimen = specimen,
                                specimenImage = specimenImage,
                                badgeText = "${index + 1} of $totalImages",
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
