package com.vci.vectorcamapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.vci.vectorcamapp.navigation.NavGraph
import com.vci.vectorcamapp.permission.presentation.PermissionAction
import com.vci.vectorcamapp.permission.presentation.PermissionEvent
import com.vci.vectorcamapp.permission.presentation.PermissionScreen
import com.vci.vectorcamapp.permission.presentation.PermissionViewModel
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: PermissionViewModel by viewModels()

    private val permissionsRequired = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA,
    )

    private val permissionLauncher = registerForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        viewModel.onAction(PermissionAction.UpdatePermissionStatus(allGranted))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VectorcamappTheme {
                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                            PermissionEvent.LaunchPermissionRequest -> {
                                permissionLauncher.launch(permissionsRequired)
                            }

                            PermissionEvent.NavigateToAppSettings -> openAppSettings()
                            PermissionEvent.NavigateToLocationSettings -> openLocationSettings()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    checkAndUpdatePermissionStatus()
                    checkAndUpdateGpsStatus()
                    viewModel.onAction(PermissionAction.RequestPermissions)
                }

                val state by viewModel.state.collectAsState()

                when (state.allGranted && state.isGpsEnabled) {
                    true -> NavGraph()
                    false -> Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        PermissionScreen(
                            state = state,
                            onAction = viewModel::onAction,
                            modifier = Modifier.padding(innerPadding)
                        )
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

        viewModel.onAction(PermissionAction.UpdatePermissionStatus(allGranted))
    }

    private fun checkAndUpdateGpsStatus() {
        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        viewModel.onAction(PermissionAction.UpdateGpsStatus(isGpsEnabled))
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
