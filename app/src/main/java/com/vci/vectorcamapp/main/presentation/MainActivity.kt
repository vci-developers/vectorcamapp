package com.vci.vectorcamapp.main.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vci.vectorcamapp.animation.presentation.LoadingAnimation
import com.vci.vectorcamapp.navigation.NavGraph
import com.vci.vectorcamapp.permission.presentation.PermissionAction
import com.vci.vectorcamapp.permission.presentation.PermissionScreen
import com.vci.vectorcamapp.permission.presentation.PermissionState
import com.vci.vectorcamapp.ui.theme.VectorcamappTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val permissionsRequired = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val all = results.values.all { it }
        viewModel.onAction(PermissionAction.UpdatePermissionStatus(all))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VectorcamappTheme {
                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            MainEvent.LaunchPermissionRequest ->
                                permissionLauncher.launch(permissionsRequired)
                            MainEvent.NavigateToAppSettings ->
                                openAppSettings()
                            MainEvent.NavigateToLocationSettings ->
                                openLocationSettings()
                        }
                    }
                }

                val st by viewModel.state.collectAsState()

                when {
                    st.isLoading -> {
                        LoadingAnimation(text = "Initializing…")
                    }
                    !st.allPermissionsGranted || !st.isGpsEnabled -> {
                        PermissionScreen(
                            state = PermissionState(
                                allGranted = st.allPermissionsGranted,
                                isGpsEnabled = st.isGpsEnabled,
                                isLoading = false
                            ),
                            onAction = viewModel::onAction
                        )
                    }
                    else -> {
                        NavGraph(startDestination = st.startDestination!!)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        viewModel.onAction(PermissionAction.UpdateGpsStatus(gpsOn))
    }

    private fun updatePermissionStatus() {
        val all = permissionsRequired.all { perm ->
            checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.onAction(PermissionAction.UpdatePermissionStatus(all))
    }

    private fun openAppSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).also(::startActivity)
    }

    private fun openLocationSettings() {
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).also(::startActivity)
    }
}
