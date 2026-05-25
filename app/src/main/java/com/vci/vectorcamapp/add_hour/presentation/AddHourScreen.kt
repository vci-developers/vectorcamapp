package com.vci.vectorcamapp.add_hour.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.core.presentation.components.button.ActionButton
import com.vci.vectorcamapp.core.presentation.components.form.DropdownField
import com.vci.vectorcamapp.core.presentation.components.form.TextEntryField
import com.vci.vectorcamapp.core.presentation.components.header.ScreenHeader
import com.vci.vectorcamapp.core.presentation.components.tile.InfoTile
import com.vci.vectorcamapp.hour_log.domain.model.HourTimeSlots
import com.vci.vectorcamapp.intake.domain.model.IntakeDropdownOptions.CollectionPlaceOption
import com.vci.vectorcamapp.ui.extensions.colors
import com.vci.vectorcamapp.ui.extensions.dimensions

@Composable
fun AddHourScreen(
    state: AddHourState,
    onAction: (AddHourAction) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenHeader(
        title = "Add Hour",
        subtitle = "Fill out the information below",
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Back",
                tint = MaterialTheme.colors.icon,
                modifier = Modifier
                    .size(MaterialTheme.dimensions.iconSizeLarge)
                    .clickable { onAction(AddHourAction.ReturnToPreviousScreen) }
            )
        },
        modifier = modifier
    ) {
        item {
            InfoTile {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                    modifier = Modifier.padding(MaterialTheme.dimensions.paddingLarge)
                ) {
                    Text(
                        text = "Collection Time",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colors.textPrimary
                    )

                    DropdownField(
                        options = HourTimeSlots.all,
                        selectedOption = state.selectedTimeSlot,
                        onOptionSelected = { onAction(AddHourAction.SelectTimeSlot(it)) },
                        label = "Select Hour",
                        modifier = Modifier.fillMaxWidth()
                    ) { timeSlot ->
                        Text(
                            text = timeSlot,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        item {
            InfoTile {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimensions.spacingMedium),
                    modifier = Modifier.padding(MaterialTheme.dimensions.paddingLarge)
                ) {
                    Text(
                        text = "Collection Information",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colors.textPrimary
                    )

                    TextEntryField(
                        value = state.wind,
                        onValueChange = { onAction(AddHourAction.EnterWind(it)) },
                        label = "Wind",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    TextEntryField(
                        value = state.rain,
                        onValueChange = { onAction(AddHourAction.EnterRain(it)) },
                        label = "Rain",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    TextEntryField(
                        value = state.relativeHumidity,
                        onValueChange = { onAction(AddHourAction.EnterRelativeHumidity(it)) },
                        label = "Relative Humidity (%)",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    TextEntryField(
                        value = state.temperature,
                        onValueChange = { onAction(AddHourAction.EnterTemperature(it)) },
                        label = "Temperature",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    DropdownField(
                        options = CollectionPlaceOption.entries,
                        selectedOption = state.selectedCollectionPlace,
                        onOptionSelected = { onAction(AddHourAction.SelectCollectionPlace(it)) },
                        label = "Collection Place",
                        modifier = Modifier.fillMaxWidth()
                    ) { place ->
                        Text(
                            text = place.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        item {
            ActionButton(
                label = "Confirm",
                onClick = { onAction(AddHourAction.ConfirmAddHour) },
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.dimensions.paddingMedium,
                    vertical = MaterialTheme.dimensions.paddingSmall
                )
            )
        }
    }
}
