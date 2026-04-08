package com.cover.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val LOCATION_TIMEOUT_MS = 5000L // 5 seconds max wait
    }

    /**
     * Get last known location quickly, or request new location if needed
     * Returns null if permission denied or location unavailable
     */
    suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        // Check permission
        if (!hasLocationPermission()) {
            return@withContext null
        }

        // Try to get last known location first (fastest)
        val lastLocation = getLastLocation()
        if (lastLocation != null && isLocationFresh(lastLocation)) {
            return@withContext lastLocation
        }

        // Request fresh location
        return@withContext requestFreshLocation()
    }

    /**
     * Get cached last location
     */
    private suspend fun getLastLocation(): Location? = suspendCancellableCoroutine { cont ->
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    cont.resume(location) {}
                }
                .addOnFailureListener {
                    cont.resume(null) {}
                }
        } catch (e: SecurityException) {
            cont.resume(null) {}
        }
    }

    /**
     * Request a fresh location update
     */
    private suspend fun requestFreshLocation(): Location? = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                1000
            ).apply {
                setWaitForAccurateLocation(false)
                setMinUpdateIntervalMillis(100)
                setMaxUpdateDelayMillis(1000)
            }.build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        fusedLocationClient.removeLocationUpdates(this)
                        cont.resume(location) {}
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )

                // Cancel listener on coroutine cancellation
                cont.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(callback)
                }
            } catch (e: SecurityException) {
                cont.resume(null) {}
            }
        }
    }

    /**
     * Check if cached location is fresh enough (within last 5 minutes)
     */
    private fun isLocationFresh(location: Location): Boolean {
        val age = System.currentTimeMillis() - location.time
        return age < 5 * 60 * 1000 // 5 minutes
    }

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
