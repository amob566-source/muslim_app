package com.universalazan.app.data.locations

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.universalazan.app.domain.location.isValidLatitude
import com.universalazan.app.domain.location.isValidLongitude
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Gets a one-off position from Android's built-in location service (no Google Play Services,
 * no internet). Only used when the user taps "Use current location".
 */
class DeviceLocationProvider(private val context: Context) {

    sealed interface Result {
        data class Success(val latitude: Double, val longitude: Double) : Result
        data object NoPermission : Result
        data object LocationOff : Result
        data object NoFix : Result
    }

    fun hasPermission(): Boolean = granted(Manifest.permission.ACCESS_FINE_LOCATION) ||
        granted(Manifest.permission.ACCESS_COARSE_LOCATION)

    suspend fun currentLocation(): Result {
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!fine && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) return Result.NoPermission
        val manager = context.getSystemService(LocationManager::class.java) ?: return Result.NoFix
        if (!LocationManagerCompat.isLocationEnabled(manager)) return Result.LocationOff
        val provider = chooseProvider(manager, fine) ?: return Result.NoFix

        val location = try {
            recentLastKnown(manager, provider)
                ?: withTimeoutOrNull(FIX_TIMEOUT_MS) { awaitFirstFix(manager, provider) }
        } catch (_: SecurityException) {
            return Result.NoPermission
        } ?: return Result.NoFix

        if (!isValidLatitude(location.latitude) || !isValidLongitude(location.longitude)) {
            return Result.NoFix
        }
        return Result.Success(location.latitude, location.longitude)
    }

    /**
     * GPS works without internet but needs precise permission. With only approximate
     * permission, fall back to the fused (Android 12+) or network provider.
     */
    private fun chooseProvider(manager: LocationManager, fine: Boolean): String? {
        fun enabled(provider: String) =
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        return when {
            fine && enabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                enabled(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
            enabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    // Permission is checked in currentLocation() before this is called.
    @SuppressLint("MissingPermission")
    private fun recentLastKnown(manager: LocationManager, provider: String): Location? {
        val last = manager.getLastKnownLocation(provider) ?: return null
        val ageNanos = SystemClock.elapsedRealtimeNanos() - last.elapsedRealtimeNanos
        return last.takeIf { ageNanos in 0..MAX_LAST_KNOWN_AGE_NANOS }
    }

    // Permission is checked in currentLocation() before this is called.
    @SuppressLint("MissingPermission")
    private suspend fun awaitFirstFix(manager: LocationManager, provider: String): Location =
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListenerCompat {
                override fun onLocationChanged(location: Location) {
                    LocationManagerCompat.removeUpdates(manager, this)
                    if (continuation.isActive) continuation.resume(location)
                }
            }
            val request = LocationRequestCompat.Builder(UPDATE_INTERVAL_MS)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .build()
            continuation.invokeOnCancellation { LocationManagerCompat.removeUpdates(manager, listener) }
            LocationManagerCompat.requestLocationUpdates(
                manager,
                provider,
                request,
                ContextCompat.getMainExecutor(context),
                listener,
            )
        }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        /** Without internet assistance, a first GPS fix can take a couple of minutes. */
        const val FIX_TIMEOUT_MS = 120_000L
        const val UPDATE_INTERVAL_MS = 1_000L
        const val MAX_LAST_KNOWN_AGE_NANOS = 2L * 60 * 1_000_000_000
    }
}
