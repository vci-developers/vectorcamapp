package com.vci.vectorcamapp

import android.Manifest
import androidx.compose.runtime.CompositionLocalProvider
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.window.layout.WindowMetricsCalculator
import com.vci.vectorcamapp.core.presentation.util.ObserveAsEvents
import com.vci.vectorcamapp.main.presentation.MainAction
import com.vci.vectorcamapp.main.presentation.MainEvent
import com.vci.vectorcamapp.main.presentation.MainViewModel
import com.vci.vectorcamapp.main.presentation.SplashScreen
import com.vci.vectorcamapp.main.presentation.PermissionScreen
import com.vci.vectorcamapp.core.presentation.util.error.LocalErrorFormatter
import com.vci.vectorcamapp.core.presentation.util.error.LocalErrorMessageEmitter
import com.vci.vectorcamapp.core.presentation.util.error.toString
import com.vci.vectorcamapp.navigation.NavGraph
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import com.vci.vectorcamapp.ui.theme.getWindowType
import dagger.hilt.android.AndroidEntryPoint
import com.vci.vectorcamapp.core.presentation.util.error.ErrorMessageEmitter
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var errorMessageEmitter: ErrorMessageEmitter

    private val viewModel: MainViewModel by viewModels()

    private val permissionsRequired = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        viewModel.onAction(MainAction.UpdatePermissionStatus(allGranted))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val widthDp = metrics.bounds.width() / resources.displayMetrics.density
        val windowType = getWindowType(widthDp)

        setContent {
            CompositionLocalProvider(
                LocalErrorMessageEmitter provides errorMessageEmitter,
                LocalErrorFormatter provides { error, context -> error.toString(context) }
            ) {
                VectorcamappTheme(windowType = windowType) {
            
                val state by viewModel.state.collectAsState()

                val isReady = state.permissionChecked && state.gpsChecked

                ObserveAsEvents(events = viewModel.events) { event ->
                    when (event) {
                        MainEvent.LaunchPermissionRequest -> permissionLauncher.launch(permissionsRequired)
                        MainEvent.NavigateToAppSettings -> openAppSettings()
                        MainEvent.NavigateToLocationSettings -> openLocationSettings()
                    }
                }

                when {
                    !isReady ->{
                        SplashScreen(modifier = Modifier.fillMaxSize())
                    }
                    state.allGranted && state.isGpsEnabled -> {
                        when (val startDestination = state.startDestination) {
                            null -> SplashScreen(modifier = Modifier.fillMaxSize())
                            else -> NavGraph(startDestination = startDestination)
                        }
                    }
                    else -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            PermissionScreen(
                                state = state,
                                onAction = viewModel::onAction,
                                modifier = Modifier
                                    .padding(innerPadding)
                            )
                        }
                    }
                }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        checkAndUpdatePermissionStatus()
        checkAndUpdateGpsStatus()
    }

    private fun checkAndUpdatePermissionStatus() {
        val allGranted = permissionsRequired.all { permission ->
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }

        viewModel.onAction(MainAction.UpdatePermissionStatus(allGranted))
    }

    private fun checkAndUpdateGpsStatus() {
        val locationManager =
            application.getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled =
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            )

        viewModel.onAction(MainAction.UpdateGpsStatus(isGpsEnabled))
    }

    private fun openAppSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).also(::startActivity)
    }

    private fun openLocationSettings() {
        Intent(
            Settings.ACTION_LOCATION_SOURCE_SETTINGS
        ).also(::startActivity)
    }
}
