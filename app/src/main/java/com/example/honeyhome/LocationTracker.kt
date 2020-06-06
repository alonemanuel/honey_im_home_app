package com.example.honeyhome

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
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

    init {

    }

    fun startTracking() {
        isTracking = true
        getPermissionAndTrack()
        sendBroadcast(activity)
    }

    fun getPermissionAndTrack() {
        val hasLocationPermission: Boolean = ActivityCompat.checkSelfPermission(
            activity, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            Toast.makeText(activity.applicationContext, "permission granted", Toast.LENGTH_LONG)
                .show()
            fusedLocationClient = getFusedLocationProviderClient(activity)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // Got last known location. In some rare situations this can be null.
                    if (location == null) {
                        Toast.makeText(
                            activity,
                            "Location is null for some reason",
                            Toast.LENGTH_LONG
                        )
                            .show()

                    } else {
                        longtitude = location.longitude
                        latitude = location.latitude
                        accuracy = location.accuracy

                    }
                }


        } else {
            val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(
                activity,
                permissions,
                MY_PERMISSIONS_REQUEST_LOCATION
            )
            Toast.makeText(activity, "permission denied", Toast.LENGTH_LONG).show()


        }
    }


    fun sendBroadcast(context: Context) {
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