package com.example.honeyhome

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import timber.log.Timber


class LocationTracker(var activity: MainActivity) : GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {
    var longtitude: Double = 0.0
    var latitude: Double = 0.0
    var accuracy: Float = 0f
    var isTracking: Boolean = false
    var hasHomeLocation: Boolean = false

    var homeLongtitude: Double = 0.0
    var homeLatitude: Double = 0.0

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
//
//    protected fun LocationTracker(parcel: Parcel) {
//
//    }

    fun updateLocation(location: Location) {
        longtitude = location.longitude
        latitude = location.latitude
        accuracy = location.accuracy
        Timber.i("Updating location with: [long=${longtitude}, lat=${latitude}, acc=${accuracy}]")
        sendBroadcast(activity)
    }

    fun setHomeLocation(latitude: Double, longtitude: Double) {
        hasHomeLocation = true
        homeLongtitude = longtitude
        homeLatitude = latitude
    }

    fun clearHomeLocation() {
        hasHomeLocation = false
        homeLongtitude = 0.0
        homeLatitude = 0.0
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

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun getPermissionAndTrack(): Boolean {
        val hasLocationPermission: Boolean = ActivityCompat.checkSelfPermission(
            activity, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when {
            hasLocationPermission -> {
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
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                begForPermissions()
                return false
            }
            else -> {

                requestPermissionsWrapper()
                return false
            }

        }
    }

    fun requestPermissionsWrapper() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    fun begForPermissions() {

        val builder1 =
            AlertDialog.Builder(activity)
        builder1.setMessage("We need your permission to let us track your location. We promise we won't do anything else with it!")
        builder1.setCancelable(true)

        builder1.setPositiveButton(
            "OK"
        ) { dialog, id -> requestPermissionsWrapper() }

        builder1.setNegativeButton(
            "NO THANKS"
        ) { dialog, id -> dialog.cancel() }

        val alert11 = builder1.create()
        alert11.show()

//
//        var result: Boolean = false
//        val builder: AlertDialog.Builder =
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert)
//            } else {
//                AlertDialog.Builder(activity)
//            }
//        builder.setTitle("Permissions needed")
//            .setMessage("We need your permission to let us track your location. We promise we won't do anything else with it!")
//            .setPositiveButton("OK") { dialog, which -> result = true }
//            .setNegativeButton("NO THANKS") { dialog, which -> result = false }
//            .setIcon(R.drawable.ic_launcher_foreground)
//            .show()
//        return result
    }

    fun sendBroadcast(context: Context) {
        Timber.i("Sending broadcast")
        val intent = Intent()
        intent.action = "start_tracking"
        context.sendBroadcast(intent)

    }


    fun stopTracking() {
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sendBroadcast(activity)
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