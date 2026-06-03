package com.vci.vectorcamapp.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vci.vectorcamapp.collection_batch.form.presentation.CollectionBatchFormEvent
import com.vci.vectorcamapp.collection_batch.form.presentation.CollectionBatchFormScreen
import com.vci.vectorcamapp.collection_batch.form.presentation.CollectionBatchFormViewModel
import com.vci.vectorcamapp.collection_batch.list.presentation.CollectionBatchListEvent
import com.vci.vectorcamapp.collection_batch.list.presentation.CollectionBatchListScreen
import com.vci.vectorcamapp.collection_batch.list.presentation.CollectionBatchListViewModel
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsEvent
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsScreen
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsViewModel
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListEvent
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListScreen
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListViewModel
import com.vci.vectorcamapp.core.logging.VectorAnalytics
import com.vci.vectorcamapp.core.presentation.components.scaffold.BaseScaffold
import com.vci.vectorcamapp.core.presentation.util.ObserveAsEvents
import com.vci.vectorcamapp.imaging.presentation.ImagingEvent
import com.vci.vectorcamapp.imaging.presentation.ImagingScreen
import com.vci.vectorcamapp.imaging.presentation.ImagingViewModel
import com.vci.vectorcamapp.incomplete_session.presentation.IncompleteSessionEvent
import com.vci.vectorcamapp.incomplete_session.presentation.IncompleteSessionScreen
import com.vci.vectorcamapp.incomplete_session.presentation.IncompleteSessionViewModel
import com.vci.vectorcamapp.intake.presentation.IntakeEvent
import com.vci.vectorcamapp.intake.presentation.IntakeScreen
import com.vci.vectorcamapp.intake.presentation.IntakeViewModel
import com.vci.vectorcamapp.landing.presentation.LandingEvent
import com.vci.vectorcamapp.landing.presentation.LandingScreen
import com.vci.vectorcamapp.landing.presentation.LandingViewModel
import com.vci.vectorcamapp.main.presentation.SplashScreen
import com.vci.vectorcamapp.registration.presentation.RegistrationEvent
import com.vci.vectorcamapp.registration.presentation.RegistrationScreen
import com.vci.vectorcamapp.registration.presentation.RegistrationViewModel
import com.vci.vectorcamapp.settings.presentation.SettingsEvent
import com.vci.vectorcamapp.settings.presentation.SettingsScreen
import com.vci.vectorcamapp.settings.presentation.SettingsViewModel

/** Maps a back-stack entry's route to a human-readable screen name for analytics. */
private fun NavBackStackEntry.analyticsScreenName(): String {
    val route = destination.route ?: return "Unknown"
    // Type-safe nav routes are fully-qualified class names; take the last segment
    val simpleName = route
        .substringAfterLast(".")
        .substringBefore("?")
        .substringBefore("/")
    // Insert spaces before each capital letter: "CompleteSessionList" → "Complete Session List"
    return simpleName.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
}

@Composable
fun NavGraph(startDestination: Destination) {
    val navController = rememberNavController()

    // ── Automatic screen view + time-on-screen tracking ──────────────────────
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry) {
        currentEntry?.analyticsScreenName()?.let { VectorAnalytics.screenView(it) }
    }

    NavHost(
        navController = navController, startDestination = startDestination
    ) {
        composable<Destination.Registration> {
            val viewModel = hiltViewModel<RegistrationViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    RegistrationEvent.NavigateToLandingScreen -> {
                        navController.navigate(Destination.Landing) {
                            popUpTo(Destination.Registration) { inclusive = true }
                        }
                    }
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                RegistrationScreen(
                    state = state, onAction = viewModel::onAction, modifier = Modifier
                )
            }
        }

        composable<Destination.Landing> {
            val viewModel = hiltViewModel<LandingViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    is LandingEvent.NavigateToIntakeScreen -> navController.navigate(
                        Destination.Intake(event.sessionType)
                    )

                    LandingEvent.NavigateToIncompleteSessionsScreen -> navController.navigate(
                        Destination.IncompleteSession
                    )

                    LandingEvent.NavigateToCompleteSessionsScreen -> navController.navigate(
                        Destination.CompleteSessionList
                    )

                    LandingEvent.NavigateBackToRegistrationScreen -> navController.popBackStack(
                        Destination.Registration, false
                    )

                    LandingEvent.NavigateToSettingsScreen -> navController.navigate(
                        Destination.Settings
                    )
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                when (state.isLoading) {
                    true -> SplashScreen(modifier = Modifier.fillMaxSize())

                    false -> LandingScreen(
                        state = state, onAction = viewModel::onAction
                    )
                }
            }
        }

        composable<Destination.Intake> {
            val viewModel = hiltViewModel<IntakeViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    is IntakeEvent.NavigateAfterIntake -> navController.navigate(event.destination)

                    IntakeEvent.NavigateBackToPreviousScreen -> navController.popBackStack()

                    IntakeEvent.NavigateBackToRegistrationScreen -> navController.popBackStack(
                        Destination.Registration, false
                    )
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                when (state.isLoading) {
                    true -> SplashScreen()

                    false -> IntakeScreen(
                        state = state, onAction = viewModel::onAction
                    )
                }
            }
        }

        composable<Destination.Imaging> {
            val viewModel = hiltViewModel<ImagingViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    ImagingEvent.NavigateBackToLandingScreen -> {
                        navController.popBackStack(Destination.Landing, false)
                    }

                    is ImagingEvent.NavigateBackToCollectionBatchListScreen -> {
                        navController.popBackStack(
                            Destination.CollectionBatchList(sessionId = event.sessionId.toString()),
                            inclusive = false,
                        )
                    }
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                when (state.isLoading) {
                    true -> SplashScreen()

                    false -> ImagingScreen(
                        state = state, onAction = viewModel::onAction
                    )
                }
            }
        }

        composable<Destination.CollectionBatchList> {
            val viewModel = hiltViewModel<CollectionBatchListViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    CollectionBatchListEvent.NavigateBackToLandingScreen -> navController.popBackStack(Destination.Landing, false)

                    is CollectionBatchListEvent.NavigateToCollectionBatchForm -> navController.navigate(
                        Destination.CollectionBatchForm(
                            sessionId = event.sessionId.toString(),
                            sessionUnitId = event.sessionUnitId?.toString()
                        )
                    )
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                when (state.isLoading) {
                    true -> SplashScreen()

                    false -> CollectionBatchListScreen(
                        state = state, onAction = viewModel::onAction
                    )
                }
            }
        }

        composable<Destination.CollectionBatchForm> {
            val viewModel = hiltViewModel<CollectionBatchFormViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    CollectionBatchFormEvent.NavigateBackToCollectionBatchListScreen ->
                        navController.popBackStack()

                    is CollectionBatchFormEvent.NavigateToImagingScreen ->
                        navController.navigate(
                            Destination.Imaging(sessionUnitId = event.sessionUnitId.toString())
                        )
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                when (state.isLoading) {
                    true -> SplashScreen()

                    false -> CollectionBatchFormScreen(
                        state = state, onAction = viewModel::onAction,
                    )
                }
            }
        }

        composable<Destination.IncompleteSession> {
            val viewModel = hiltViewModel<IncompleteSessionViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    IncompleteSessionEvent.NavigateBackToLandingScreen -> navController.popBackStack()

                    is IncompleteSessionEvent.NavigateToIntakeScreen -> navController.navigate(
                        Destination.Intake(event.sessionType)
                    )
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                IncompleteSessionScreen(
                    state = state, onAction = viewModel::onAction
                )
            }
        }

        composable<Destination.CompleteSessionList> {
            val viewModel = hiltViewModel<CompleteSessionListViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    CompleteSessionListEvent.NavigateBackToLandingScreen -> navController.popBackStack()

                    is CompleteSessionListEvent.NavigateToCompleteSessionDetails -> navController.navigate(
                        Destination.CompleteSessionDetails(event.sessionId.toString())
                    )
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                CompleteSessionListScreen(
                    state = state, onAction = viewModel::onAction
                )
            }
        }

        composable<Destination.CompleteSessionDetails> {
            val viewModel = hiltViewModel<CompleteSessionDetailsViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    CompleteSessionDetailsEvent.NavigateBackToCompleteSessionListScreen -> navController.popBackStack()
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                CompleteSessionDetailsScreen(
                    state = state, onAction = viewModel::onAction
                )
            }
        }

        composable<Destination.Settings> {
            val viewModel = hiltViewModel<SettingsViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    is SettingsEvent.NavigateToIntakeScreen -> navController.navigate(
                        Destination.Intake(event.sessionType)
                    )

                    SettingsEvent.NavigateBackToLandingScreen -> navController.popBackStack(
                        Destination.Landing, false
                    )
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(
                    state = state, onAction = viewModel::onAction
                )
            }
        }
    }
}
