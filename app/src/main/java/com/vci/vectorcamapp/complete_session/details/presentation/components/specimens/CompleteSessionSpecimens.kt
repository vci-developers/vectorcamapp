package com.vci.vectorcamapp.complete_session.details.presentation.components.specimens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions
import com.vci.vectorcamapp.ui.theme.screenWidthFraction

@Composable
fun CompleteSessionSpecimens(
    session: Session,
    specimensWithImagesAndInferenceResults: List<SpecimenWithSpecimenImagesAndInferenceResults>,
    searchQuery: String,
    onUpdateSearchQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    if (specimensWithImagesAndInferenceResults.isEmpty() && searchQuery.isBlank()) {
        Text(
            "No specimens were captured during this session.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(MaterialTheme.dimensions.paddingMedium)
        )
    } else {
        val itemsToDisplay = specimensWithImagesAndInferenceResults.flatMap { specimenWithImagesAndInferenceResults ->
            val totalImageCount = specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.size
            specimenWithImagesAndInferenceResults.specimenImagesAndInferenceResults.mapIndexed { imageIndex, imageAndInferenceResult ->
                Triple(
                    specimenWithImagesAndInferenceResults.specimen,
                    imageAndInferenceResult.specimenImage,
                    "${imageIndex + 1} of $totalImageCount"
                )
            }
        }

        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextEntryField(
                value = searchQuery,
                onValueChange = { newSearchQuery -> onUpdateSearchQuery(newSearchQuery) },
                placeholder = "Search by specimen ID, species, etc.",
                modifier = Modifier.padding(horizontal = MaterialTheme.dimensions.spacingMedium),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    }
                )
            )

            if (itemsToDisplay.isEmpty()) {
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
                    items(
                        items = itemsToDisplay,
                        key = { specimenImageTile -> specimenImageTile.second.localId }
                    ) { specimenImageTile  ->
                        CompleteSessionSpecimensTile(
                            session = session,
                            specimen = specimenImageTile.first,
                            specimenImage = specimenImageTile.second,
                            badgeText = specimenImageTile.third,
                            modifier = Modifier.width(screenWidthFraction(0.9f))
                        )
                    }
                }
            }
        }
    }
}
