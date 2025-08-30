package com.vci.vectorcamapp.complete_session.details.presentation.components.specimens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.complete_session.domain.model.SpecimenImageItem
import com.vci.vectorcamapp.core.domain.model.Session
import com.vci.vectorcamapp.core.domain.model.composites.SpecimenWithSpecimenImagesAndInferenceResults
import com.vci.vectorcamapp.core.presentation.util.search.SearchUtils
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.theme.screenWidthFraction

@Composable
fun CompleteSessionSpecimens(
    session: Session,
    specimenResults: List<SpecimenWithSpecimenImagesAndInferenceResults>,
    executedQuery: String,
    onPerformSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by rememberSaveable { mutableStateOf(executedQuery) }

    if (specimenResults.isEmpty()) {
        Text(
            "No specimens were captured during this session.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxSize()
        )
    } else {
        val filteredSpecimenResults = specimenResults.filter { specimenGroup ->
                val fieldsForSearch = buildList {
                    add(specimenGroup.specimen.id)
                    specimenGroup.specimenImagesAndInferenceResults.forEach { imageAndInferenceResult ->
                        add(imageAndInferenceResult.specimenImage.species)
                        add(imageAndInferenceResult.specimenImage.sex)
                        add(imageAndInferenceResult.specimenImage.abdomenStatus)
                    }
                }
                SearchUtils.matchesQuery(executedQuery, fieldsForSearch)
            }

        val itemsToDisplay = filteredSpecimenResults.flatMap { result ->
            val totalImageCount = result.specimenImagesAndInferenceResults.size
            result.specimenImagesAndInferenceResults.mapIndexed { index, imageAndResult ->
                SpecimenImageItem(
                    specimen = result.specimen,
                    specimenImage = imageAndResult.specimenImage,
                    badgeText = "${index + 1} of $totalImageCount"
                )
            }
        }

        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newSearchQuery -> searchQuery = newSearchQuery },
                label = { Text("Search by specimen ID, species, etc.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onPerformSearch(searchQuery)
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
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(
                        items = itemsToDisplay,
                        key = { it.specimenImage.localId }
                    ) { item ->
                        CompleteSessionSpecimensTile(
                            session = session,
                            specimen = item.specimen,
                            specimenImage = item.specimenImage,
                            badgeText = item.badgeText,
                            modifier = Modifier.width(screenWidthFraction(0.9f))
                        )
                    }
                }
            }
        }
    }
}
