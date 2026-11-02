package com.vci.vectorcamapp.intake.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vci.vectorcamapp.R
import com.vci.vectorcamapp.intake.presentation.IntakeAction
import com.vci.vectorcamapp.intake.presentation.IntakeState

@Composable
internal fun IntakeGeographicalInfoSection(
    state: IntakeState,
    onAction: (IntakeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val distanceToSiteMeters = rememberDistanceToSiteMeters(state)
    IntakeTile(
        title = stringResource(R.string.intake_title_geographical_info),
        iconPainter = painterResource(id = R.drawable.ic_pin),
        iconDescription = stringResource(R.string.intake_content_description_geographical_info),
        modifier = modifier,
    ) {
        SiteSelectionFields(state = state, onAction = onAction)
        LocationSummaryCards(
            state = state,
            distanceToSiteMeters = distanceToSiteMeters,
            onAction = onAction,
        )
    }
}
