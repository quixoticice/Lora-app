package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.model.GpsPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GpsService(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _currentLocation = MutableStateFlow<GpsPayload?>(null)
    val currentLocation: StateFlow<GpsPayload?> = _currentLocation

    private val _isGpsEnabled = MutableStateFlow(false)
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _currentLocation.value = GpsPayload(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                altitude = location.altitude,
                speed = location.speed
            )
        }

        override fun onProviderEnabled(provider: String) {
            _isGpsEnabled.value = true
        }

        override fun onProviderDisabled(provider: String) {
            _isGpsEnabled.value = false
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            val isGps = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            val isNetwork = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            _isGpsEnabled.value = isGps || isNetwork

            // Get last known location immediately
            var lastLoc: Location? = null
            if (isGps) {
                lastLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (lastLoc == null && isNetwork) {
                lastLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            lastLoc?.let { loc ->
                _currentLocation.value = GpsPayload(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    altitude = loc.altitude,
                    speed = loc.speed
                )
            }

            if (isGps) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    5f,
                    locationListener
                )
            } else if (isNetwork) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    locationListener
                )
            }
        } catch (e: Exception) {
            // Handle permissions or disabled GPS
        }
    }

    fun stopLocationUpdates() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun getCurrentOrFallbackLocation(): GpsPayload {
        return _currentLocation.value ?: GpsPayload(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 12.5f,
            altitude = 45.0,
            speed = 0.0f,
            note = "Estimated GPS"
        )
    }
}
