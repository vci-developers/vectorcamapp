package com.vci.vectorcamapp.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsEvent
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsScreen
import com.vci.vectorcamapp.complete_session.details.presentation.CompleteSessionDetailsViewModel
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListEvent
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListScreen
import com.vci.vectorcamapp.complete_session.list.presentation.CompleteSessionListViewModel
import com.vci.vectorcamapp.core.presentation.components.scaffold.BaseScaffold
import com.vci.vectorcamapp.core.presentation.util.ObserveAsEvents
import com.vci.vectorcamapp.imaging.presentation.ImagingEvent
import com.vci.vectorcamapp.imaging.presentation.ImagingScreen
import com.vci.vectorcamapp.imaging.presentation.ImagingViewModel
import com.vci.vectorcamapp.incomplete_session.presentation.IncompleteSessionEvent
import com.vci.vectorcamapp.incomplete_session.presentation.IncompleteSessionScreen
import com.vci.vectorcamapp.incomplete_session.presentation.IncompleteSessionViewModel
import com.vci.vectorcamapp.landing.presentation.LandingEvent
import com.vci.vectorcamapp.landing.presentation.LandingScreen
import com.vci.vectorcamapp.landing.presentation.LandingViewModel
import com.vci.vectorcamapp.registration.presentation.RegistrationEvent
import com.vci.vectorcamapp.registration.presentation.RegistrationScreen
import com.vci.vectorcamapp.registration.presentation.RegistrationViewModel
import com.vci.vectorcamapp.intake.presentation.IntakeEvent
import com.vci.vectorcamapp.intake.presentation.IntakeScreen
import com.vci.vectorcamapp.intake.presentation.IntakeViewModel
import com.vci.vectorcamapp.main.presentation.SplashScreen
import com.vci.vectorcamapp.settings.presentation.SettingsEvent
import com.vci.vectorcamapp.settings.presentation.SettingsScreen
import com.vci.vectorcamapp.settings.presentation.SettingsViewModel

@Composable
fun NavGraph(startDestination: Destination) {
    val navController = rememberNavController()

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
                    state = state,
                    onAction = viewModel::onAction,
                    modifier = Modifier
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
                        state = state,
                        onAction = viewModel::onAction
                    )
                }
            }
        }

        composable<Destination.Intake> {
            val viewModel = hiltViewModel<IntakeViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ObserveAsEvents(events = viewModel.events) { event ->
                when (event) {
                    IntakeEvent.NavigateToImagingScreen -> navController.navigate(
                        Destination.Imaging
                    )

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
                        state = state,
                        onAction = viewModel::onAction
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
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                when (state.isLoading) {
                    true -> SplashScreen()
                    
                    false -> ImagingScreen(
                        state = state,
                        onAction = viewModel::onAction
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

                    is IncompleteSessionEvent.NavigateToIntakeScreen ->
                        navController.navigate(Destination.Intake(event.sessionType))
                }
            }

            BaseScaffold(modifier = Modifier.fillMaxSize()) {
                IncompleteSessionScreen(
                    state = state,
                    onAction = viewModel::onAction
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
                    state = state,
                    onAction = viewModel::onAction
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
                    state = state,
                    onAction = viewModel::onAction
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
                    state = state,
                    onAction = viewModel::onAction
                )
            }
        }
    }
}
