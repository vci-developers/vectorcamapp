package com.vci.vectorcamapp.complete_session.details.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.complete_session.details.presentation.enums.CompleteSessionDetailsTab
import com.vci.vectorcamapp.complete_session.details.presentation.components.form.CompleteSessionFormScreen
import com.vci.vectorcamapp.complete_session.details.presentation.components.specimens.CompleteSessionSpecimensScreen

@Composable
fun CompleteSessionDetailsScreen(
    state: CompleteSessionDetailsState,
    onAction: (CompleteSessionDetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {

    Column {
        TabRow(selectedTabIndex = state.selectedTab.ordinal) {
            Tab(
                selected = state.selectedTab == CompleteSessionDetailsTab.SESSION_FORM,
                onClick = { onAction(CompleteSessionDetailsAction.ChangeSelectedTab(CompleteSessionDetailsTab.SESSION_FORM)) },
                text = { Text("Details") })
            Tab(
                selected = state.selectedTab == CompleteSessionDetailsTab.SESSION_SPECIMENS,
                onClick = { onAction(CompleteSessionDetailsAction.ChangeSelectedTab(CompleteSessionDetailsTab.SESSION_SPECIMENS)) },
                text = { Text("Specimens") })
        }

        when (state.selectedTab) {
            CompleteSessionDetailsTab.SESSION_FORM -> CompleteSessionFormScreen(session = state.session, site = state.site, surveillanceForm = state.surveillanceForm, modifier = modifier)
            CompleteSessionDetailsTab.SESSION_SPECIMENS -> CompleteSessionSpecimensScreen(session = state.session, specimens = state.specimens, modifier = modifier)
        }
    }
}
