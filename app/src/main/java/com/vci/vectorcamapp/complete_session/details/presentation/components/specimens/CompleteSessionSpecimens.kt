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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsAction
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsState
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.theme.screenWidthFraction

@Composable
fun CompleteSessionSpecimens(
    state: CompleteSessionDetailsState,
    onAction: (CompleteSessionDetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    if (state.specimensWithImagesAndInferenceResults.isEmpty()) {
        Text(
            "No specimens were captured during this session.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onAction(CompleteSessionDetailsAction.UpdateQuery(it)) },
            label = { Text("Search by specimen ID, species, etc.") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onAction(CompleteSessionDetailsAction.ExecuteQuery)
                    keyboardController?.hide()
                }
            )
        )

        if (state.filteredSpecimenImageItems.isEmpty()) {
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
                    items = state.filteredSpecimenImageItems,
                    key = { it.specimenImage.localId }
                ) { item ->
                    CompleteSessionSpecimensTile(
                        session = state.session,
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
