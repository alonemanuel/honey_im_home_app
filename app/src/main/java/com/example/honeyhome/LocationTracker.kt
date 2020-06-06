package com.example.honeyhome

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import timber.log.Timber

class LocationTracker(activity: MainActivity) : GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {
    var longtitude: Double = 0.0
    var latitude: Double = 0.0
    var accuracy: Float = 0f
    val activity: MainActivity = activity
    var isTracking: Boolean = false
    private val MY_PERMISSIONS_REQUEST_LOCATION = 99;
    lateinit var fusedLocationClient: FusedLocationProviderClient
    var locationRequest = LocationRequest.create()?.apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    var locationCallback: LocationCallback

    init {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    updateLocation(location)
                }
            }
        }
    }

    fun updateLocation(location: Location) {
        longtitude = location.longitude
        latitude = location.latitude
        accuracy = location.accuracy
        Timber.i("Updating location with: [long=${longtitude}, lat=${latitude}, acc=${accuracy}]")
        sendBroadcast(activity)
    }

    fun startTracking() {
        Timber.i("In startTracking()")
        if (getPermissionAndTrack()) {
            isTracking = true
            startLocationUpdates()
        }
    }

    fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun getPermissionAndTrack(): Boolean {
        val hasLocationPermission: Boolean = ActivityCompat.checkSelfPermission(
            activity, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            Timber.i("Permission granted")
            fusedLocationClient = getFusedLocationProviderClient(activity)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // Got last known location. In some rare situations this can be null.
                    if (location == null) {
                        Timber.i(
                            "Location is null for some reason"
                        )
                    } else {
                        updateLocation(location)
                    }
                }
            return true

        } else {
            val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(
                activity,
                permissions,
                MY_PERMISSIONS_REQUEST_LOCATION
            )
            Toast.makeText(activity, "Permission denied", Toast.LENGTH_LONG).show()
            return false

        }
    }


    fun sendBroadcast(context: Context) {
        Timber.i("Sending broadcast")
        val intent = Intent()
        intent.action = "start_tracking"
        context.sendBroadcast(intent)

    }


    fun stopTracking() {
        isTracking = false
    }

    override fun onConnected(p0: Bundle?) {
        Timber.i("connected")
    }

    override fun onConnectionSuspended(p0: Int) {
        Timber.i("Connection suspended, please reconnect")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Timber.i("connection failed")
    }


}