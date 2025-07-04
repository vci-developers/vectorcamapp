package com.vci.vectorcamapp.surveillance_form.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.vci.vectorcamapp.surveillance_form.domain.enums.CollectionMethodOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.DistrictOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.LlinBrandOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.LlinTypeOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.SentinelSiteOption
import com.vci.vectorcamapp.surveillance_form.domain.enums.SpecimenConditionOption
import com.vci.vectorcamapp.surveillance_form.location.data.LocationError
import com.vci.vectorcamapp.surveillance_form.location.data.toString
import com.vci.vectorcamapp.surveillance_form.presentation.components.DatePickerField
import com.vci.vectorcamapp.surveillance_form.presentation.components.DropdownField
import com.vci.vectorcamapp.surveillance_form.presentation.components.TextEntryField
import com.vci.vectorcamapp.surveillance_form.presentation.components.ToggleField
import com.vci.vectorcamapp.ui.extensions.customShadow
import com.vci.vectorcamapp.ui.theme.LocalColors
import com.vci.vectorcamapp.ui.theme.LocalDimensions
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme

@Composable
fun SurveillanceFormScreen(
    state: SurveillanceFormState,
    onAction: (SurveillanceFormAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val dims = LocalDimensions.current

    val pageHeaderHeight = 0.25f
    val pageBodyOffset = 0.2f

    Scaffold (
        modifier = modifier
    ) { systemBars ->
        Box(Modifier.fillMaxSize()) {
            PageHeader(
                title = "Surveillance Form",
                onBack = { onAction(SurveillanceFormAction.SaveSessionProgress) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightFraction(pageHeaderHeight)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .offsetYFraction(pageBodyOffset)
                    .padding(horizontal = dims.paddingMedium)
                    .padding(systemBars)
            ) {

                item {
                    SectionCard(sectionTitle = "General Information") {

                        TextEntryField(
                            label = "Collector Name",
                            value = state.session.collectorName,
                            onValueChange = { onAction(SurveillanceFormAction.EnterCollectorName(it)) },
                            singleLine = true,
                            error = state.surveillanceFormErrors.collectorName
                        )
                        Spacer(Modifier.height(dims.spacingSmall))

                        TextEntryField(
                            label = "Collector Title",
                            value = state.session.collectorTitle,
                            onValueChange = { onAction(SurveillanceFormAction.EnterCollectorTitle(it)) },
                            singleLine = true,
                            error = state.surveillanceFormErrors.collectorTitle
                        )
                        Spacer(Modifier.height(dims.spacingSmall))

                        DatePickerField(
                            label = "Collection Date",
                            selectedDateInMillis = state.session.collectionDate,
                            onDateSelected = { onAction(SurveillanceFormAction.PickCollectionDate(it)) },
                            error = state.surveillanceFormErrors.collectionDate,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(dims.spacingSmall))

                        DropdownField(
                            label = "Collection Method",
                            options = CollectionMethodOption.entries,
                            selectedOption = CollectionMethodOption.entries
                                .firstOrNull { it.label == state.session.collectionMethod },
                            onOptionSelected = {
                                onAction(
                                    SurveillanceFormAction.SelectCollectionMethod(
                                        it
                                    )
                                )
                            },
                            error = state.surveillanceFormErrors.collectionMethod,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(dims.spacingSmall))

                        DropdownField(
                            label = "Specimen Condition",
                            options = SpecimenConditionOption.entries,
                            selectedOption = SpecimenConditionOption.entries
                                .firstOrNull { it.label == state.session.specimenCondition },
                            onOptionSelected = {
                                onAction(
                                    SurveillanceFormAction.SelectSpecimenCondition(
                                        it
                                    )
                                )
                            },
                            error = state.surveillanceFormErrors.specimenCondition,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    SectionCard(sectionTitle = "Geographical Information") {

                        DropdownField(
                            label = "District",
                            options = state.allSitesInProgram
                                .map { DistrictOption(it.district) }
                                .distinctBy { it.label },
                            selectedOption = DistrictOption(state.selectedDistrict),
                            onOptionSelected = { onAction(SurveillanceFormAction.SelectDistrict(it)) },
                            error = state.surveillanceFormErrors.district
                        )
                        Spacer(Modifier.height(dims.spacingSmall))

                        if (state.selectedDistrict.isNotBlank()) {
                            DropdownField(
                                label = "Sentinel Site",
                                options = state.allSitesInProgram
                                    .filter { it.district == state.selectedDistrict }
                                    .map { SentinelSiteOption(it.sentinelSite) }
                                    .distinctBy { it.label },
                                selectedOption = SentinelSiteOption(state.selectedSentinelSite),
                                onOptionSelected = {
                                    onAction(SurveillanceFormAction.SelectSentinelSite(it))
                                },
                                error = state.surveillanceFormErrors.sentinelSite
                            )
                            Spacer(Modifier.height(dims.spacingSmall))
                        }

                        TextEntryField(
                            label = "House Number",
                            value = state.session.houseNumber,
                            onValueChange = { onAction(SurveillanceFormAction.EnterHouseNumber(it)) },
                            singleLine = true,
                            error = state.surveillanceFormErrors.houseNumber
                        )
                        Spacer(Modifier.height(dims.spacingSmall))

                        TextEntryField(
                            label = "Number of House Occupants",
                            value = state.surveillanceForm.numPeopleSleptInHouse.toString(),
                            onValueChange = {
                                onAction(SurveillanceFormAction.EnterNumPeopleSleptInHouse(it))
                            },
                            singleLine = true,
                            keyboardType = KeyboardType.Number,
                        )
                        Spacer(Modifier.height(dims.spacingSmall))

                        when {
                            state.latitude != null && state.longitude != null -> {
                                Text("Latitude: ${state.latitude}")
                                Spacer(Modifier.height(dims.spacingSmall))
                                Text("Longitude: ${state.longitude}")
                            }

                            state.locationError != null -> {
                                val context = LocalContext.current
                                Text(
                                    text = "Could not get location: " +
                                            state.locationError.toString(context)
                                )
                                if (state.locationError == LocationError.GPS_TIMEOUT) {
                                    Spacer(Modifier.height(dims.spacingSmall))

                                    val colors = LocalColors.current
                                    Button(
                                        onClick = { onAction(SurveillanceFormAction.RetryLocation) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        contentPadding = PaddingValues()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        listOf(
                                                            colors.buttonGradientLeft,
                                                            colors.buttonGradientRight
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(dims.cornerRadiusMedium)
                                                )
                                                .padding(
                                                    horizontal = dims.paddingMedium,
                                                    vertical = dims.paddingSmall
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Retry Location",
                                                color = colors.buttonText,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator()
                                    Spacer(Modifier.width(dims.spacingSmall))
                                    Text("Getting location…")
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(sectionTitle = "Surveillance Form") {
                        TextEntryField(
                            label = "Number of LLINs Available",
                            value = state.surveillanceForm.numLlinsAvailable.toString(),
                            onValueChange = {
                                onAction(
                                    SurveillanceFormAction.EnterNumLlinsAvailable(
                                        it
                                    )
                                )
                            },
                            singleLine = true,
                            keyboardType = KeyboardType.Number
                        )
                        Spacer(Modifier.height(dims.spacingSmall))

                        state.surveillanceForm.llinType?.let { current ->
                            DropdownField(
                                label = "LLIN Type",
                                options = LlinTypeOption.entries,
                                selectedOption = LlinTypeOption.entries.firstOrNull { it.label == current },
                                onOptionSelected = {
                                    onAction(SurveillanceFormAction.SelectLlinType(it))
                                },
                                error = state.surveillanceFormErrors.llinType
                            )
                            Spacer(Modifier.height(dims.spacingSmall))
                        }

                        state.surveillanceForm.llinBrand?.let { current ->
                            DropdownField(
                                label = "LLIN Brand",
                                options = LlinBrandOption.entries,
                                selectedOption = LlinBrandOption.entries.firstOrNull { it.label == current },
                                onOptionSelected = {
                                    onAction(SurveillanceFormAction.SelectLlinBrand(it))
                                },
                                error = state.surveillanceFormErrors.llinBrand
                            )
                            Spacer(Modifier.height(dims.spacingSmall))
                        }

                        state.surveillanceForm.numPeopleSleptUnderLlin?.let { current ->
                            TextEntryField(
                                label = "Number of People who Slept Under LLIN",
                                value = current.toString(),
                                onValueChange = {
                                    onAction(SurveillanceFormAction.EnterNumPeopleSleptUnderLlin(it))
                                },
                                singleLine = true,
                                keyboardType = KeyboardType.Number
                            )
                            Spacer(Modifier.height(dims.spacingSmall))
                        }

                        ToggleField(
                            label = "IRS Conducted in this Household",
                            checked = state.surveillanceForm.wasIrsConducted,
                            onCheckedChange = {
                                onAction(
                                    SurveillanceFormAction.ToggleIrsConducted(
                                        it
                                    )
                                )
                            }
                        )
                        Spacer(Modifier.height(dims.spacingSmall))

                        if (state.surveillanceForm.wasIrsConducted) {
                            TextEntryField(
                                label = "Months Since IRS",
                                value = state.surveillanceForm.monthsSinceIrs?.toString().orEmpty(),
                                onValueChange = {
                                    onAction(
                                        SurveillanceFormAction.EnterMonthsSinceIrs(
                                            it
                                        )
                                    )
                                },
                                singleLine = true,
                                keyboardType = KeyboardType.Number,
                            )
                        }
                    }
                }

                item {
                    SectionCard(sectionTitle = "Optional") {
                        TextEntryField(
                            label = "Notes",
                            value = state.session.notes,
                            onValueChange = { onAction(SurveillanceFormAction.EnterNotes(it)) }
                        )
                    }
                }

                item {
                    val colors = LocalColors.current
                    Button(
                        onClick = { onAction(SurveillanceFormAction.SubmitSurveillanceForm) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dims.paddingLarge)
                            .height(dims.componentHeightLarge),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            colors.buttonGradientLeft,
                                            colors.buttonGradientRight
                                        )
                                    ),
                                    shape = RoundedCornerShape(dims.cornerRadiusMedium)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Confirm",
                                color = colors.buttonText,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.heightFraction(pageBodyOffset))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun SurveillanceFormScreenPreview() {
    VectorcamappTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SurveillanceFormScreen(
                state = SurveillanceFormState(),
                onAction = { },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun PageHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalColors.current
    val dims   = LocalDimensions.current

    Column(
        modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.headerGradientTopLeft,
                        colors.headerGradientBottomRight
                    )
                )
            )
            .padding(
                horizontal = dims.paddingMedium,
                vertical   = dims.paddingMedium
            )
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Spacer(Modifier.height(dims.spacingMedium))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(dims.spacingMedium))
            Column {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text  = "Fill out the information below",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
private fun SectionCard(
    sectionTitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dims = LocalDimensions.current
    val colors = LocalColors.current

    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = dims.paddingMedium)
            .customShadow(
                color = colors.cardGlow.copy(alpha = 0.25f),
                offsetY = dims.shadowOffsetYSmall,
                blurRadius = dims.shadowBlurMedium,
                cornerRadius = dims.cornerRadiusMedium,
                spread = 0.dp
            )
            .background(
                color = colors.cardBackground,
                shape = RoundedCornerShape(dims.cornerRadiusMedium)
            )
            .padding(dims.paddingLarge)
    ) {
        Text(
            text  = sectionTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(dims.spacingMedium))
        content()
    }
}

@Composable
fun Modifier.heightFraction(fraction: Float): Modifier {
    return this.then(
        with(LocalDensity.current) {
            Modifier.height(
                (LocalConfiguration.current.screenHeightDp * fraction).dp
            )
        }
    )
}

@Composable
fun Modifier.offsetYFraction(fraction: Float): Modifier {
    return this.then(
        with(LocalDensity.current) {
            Modifier.offset(
                y = (LocalConfiguration.current.screenHeightDp * fraction).dp
            )
        }
    )
}