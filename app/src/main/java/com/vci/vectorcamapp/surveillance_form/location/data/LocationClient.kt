package com.vci.vectorcamapp.surveillance_form.location.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


@ViewModelScoped
class LocationClient @Inject constructor(
    private val fused: FusedLocationProviderClient,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MAX_LAST_LOCATION_AGE_MS = 5 * 60 * 1_000L
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { cont ->
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cont.resumeWithException(
                SecurityException("ACCESS_FINE_LOCATION permission not granted")
            )
            return@suspendCancellableCoroutine
        }

            val cts = CancellationTokenSource()
            val now = System.currentTimeMillis()

            fused.lastLocation
                .addOnSuccessListener { last ->
                    if (last != null && now - last.time <= MAX_LAST_LOCATION_AGE_MS) {
                        if (cont.isActive) cont.resume(last)
                    } else {
                        requestFreshLocation(cts, cont)
                    }
                }
                .addOnFailureListener {
                    requestFreshLocation(cts, cont)
                }

            cont.invokeOnCancellation { cts.cancel() }
        }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun requestFreshLocation(
        cts: CancellationTokenSource,
        cont: CancellableContinuation<Location>
    ) {
        fused.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cts.token
        ).addOnSuccessListener { loc ->
            if (loc != null && cont.isActive) {
                cont.resume(loc)
            } else if (cont.isActive) {
                cont.resumeWithException(RuntimeException("No location available"))
            }
        }.addOnFailureListener { e ->
            if (cont.isActive) cont.resumeWithException(e)
        }
    }
}