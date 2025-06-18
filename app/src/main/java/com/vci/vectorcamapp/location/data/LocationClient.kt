package com.vci.vectorcamapp.location.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


@Singleton
class LocationClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val MAX_LAST_LOCATION_AGE_MS = 5 * 60 * 1_000L
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location =
        suspendCancellableCoroutine { cont ->
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

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(
        cts: CancellationTokenSource,
        cont: CancellableContinuation<Location>
    ) {
        fused.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
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