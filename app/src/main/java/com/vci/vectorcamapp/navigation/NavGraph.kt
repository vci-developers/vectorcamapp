package com.vci.vectorcamapp.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vci.vectorcamapp.core.presentation.util.ObserveAsEvents
import com.vci.vectorcamapp.imaging.presentation.util.toString
import com.vci.vectorcamapp.imaging.presentation.ImagingEvent
import com.vci.vectorcamapp.imaging.presentation.ImagingScreen
import com.vci.vectorcamapp.imaging.presentation.ImagingViewModel
import com.vci.vectorcamapp.landing.presentation.LandingEvent
import com.vci.vectorcamapp.landing.presentation.LandingScreen
import com.vci.vectorcamapp.landing.presentation.LandingViewModel
import androidx.navigation.compose.navigation
import com.vci.vectorcamapp.animation.presentation.LoadingAnimation
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsScreen
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsViewModel
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListEvent
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListScreen
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListViewModel
import com.vci.vectorcamapp.complete_session.specimens.presentation.CompleteSessionSpecimensScreen
import com.vci.vectorcamapp.complete_session.specimens.presentation.CompleteSessionSpecimensViewModel
import com.vci.vectorcamapp.incomplete_session.presentation.IncompleteSessionScreen
import com.vci.vectorcamapp.incomplete_session.presentation.IncompleteSessionViewModel
import com.vci.vectorcamapp.surveillance_form.presentation.SurveillanceFormEvent
import com.vci.vectorcamapp.surveillance_form.presentation.SurveillanceFormScreen
import com.vci.vectorcamapp.surveillance_form.presentation.SurveillanceFormViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController, startDestination = Destination.Landing
    ) {
        composable<Destination.Landing> {
            val viewModel = viewModel<LandingViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    LandingEvent.NavigateToNewSurveillanceSessionScreen -> navController.navigate(
                        Destination.SurveillanceForm
                    )

                    LandingEvent.NavigateToIncompleteSessionsScreen -> navController.navigate(
                        Destination.IncompleteSession
                    )

                    LandingEvent.NavigateToCompleteSessionsScreen -> navController.navigate(
                        Destination.CompleteSessionList
                    )
                }
            }

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (state.isLoading) {
                    true -> LoadingAnimation(
                        text = "Loading...", modifier = Modifier.padding(innerPadding)
                    )

                    false -> LandingScreen(
                        state = state,
                        onAction = viewModel::onAction,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        composable<Destination.SurveillanceForm> {
            // TODO: UPON PRESSING THE BACK BUTTON, SHOULD CLEAR THE CACHE!
            val viewModel = hiltViewModel<SurveillanceFormViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    SurveillanceFormEvent.NavigateToImagingScreen -> navController.navigate(
                        Destination.Imaging
                    )

                    SurveillanceFormEvent.NavigateBackToLandingScreen -> {
                        navController.popBackStack(Destination.Landing, false)
                    }
                }
            }

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (state.isLoading) {
                    true -> LoadingAnimation(
                        text = "Loading...", modifier = Modifier.padding(innerPadding)
                    )

                    false -> SurveillanceFormScreen(
                        state = state,
                        onAction = viewModel::onAction,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        composable<Destination.Imaging> {
            val context = LocalContext.current
            val viewModel = hiltViewModel<ImagingViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    is ImagingEvent.DisplayImagingError -> {
                        Toast.makeText(context, event.error.toString(context), Toast.LENGTH_LONG)
                            .show()
                    }

                    ImagingEvent.NavigateBackToLandingScreen -> {
                        navController.popBackStack(Destination.Landing, false)
                    }
                }
            }

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                ImagingScreen(
                    state = state,
                    onAction = viewModel::onAction,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        composable<Destination.IncompleteSession> {
            val viewModel = hiltViewModel<IncompleteSessionViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                IncompleteSessionScreen(
                    state = state,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        composable<Destination.CompleteSessionList> {
            val viewModel = hiltViewModel<CompleteSessionListViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    is CompleteSessionListEvent.NavigateToCompleteSessionDetails -> {
                        navController.navigate(
                            Destination.CompleteSessionRoot.createRoute(event.sessionId)
                        )
                    }

                    is CompleteSessionListEvent.NavigateToCompleteSessionSpecimens -> {
                        navController.navigate(
                            Destination.CompleteSessionRoot.createRoute(event.sessionId) + "/specimens"
                        )
                    }
                }
            }

            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                CompleteSessionListScreen(
                    state = state,
                    onAction = viewModel::onAction,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
        navigation(
            route = Destination.CompleteSessionRoot.ROUTE + "/{sessionId}",
            startDestination = Destination.CompleteSessionDetails.ROUTE,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            composable(Destination.CompleteSessionDetails.ROUTE) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                val selectedTab = rememberSaveable { mutableIntStateOf(0) }

                val detailsViewModel = hiltViewModel<CompleteSessionDetailsViewModel>()
                val detailsState by detailsViewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(sessionId) {
                    detailsViewModel.loadSession(sessionId)
                }

                val specimensViewModel = hiltViewModel<CompleteSessionSpecimensViewModel>()
                val specimensState by specimensViewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(sessionId) {
                    specimensViewModel.loadSession(sessionId)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        TabRow(selectedTabIndex = selectedTab.intValue) {
                            Tab(
                                selected = selectedTab.intValue == 0,
                                onClick = { selectedTab.intValue = 0 },
                                text = { Text("Details") }
                            )
                            Tab(
                                selected = selectedTab.intValue == 1,
                                onClick = { selectedTab.intValue = 1 },
                                text = { Text("Specimens") }
                            )
                        }
                        when (selectedTab.intValue) {
                            0 -> CompleteSessionDetailsScreen(state = detailsState)
                            1 -> CompleteSessionSpecimensScreen(state = specimensState)
                        }
                    }
                }
            }
        }
    }
}
