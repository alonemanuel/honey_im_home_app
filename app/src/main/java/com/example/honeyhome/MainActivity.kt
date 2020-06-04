package com.example.honeyhome

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import timber.log.Timber


class MainActivity : GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, AppCompatActivity() {
    public lateinit var locationTracker: LocationTracker
    private lateinit var locationCallback: LocationCallback

    lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mGoogleApiClient: GoogleApiClient? = null

    private final val MY_PERMISSIONS_REQUEST_LOCATION = 99;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var activity: MainActivity = this
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                p0 ?: return
                for (location in p0.locations) {
                    Toast.makeText(activity, location.longitude.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
                super.onLocationResult(p0)
            }
        }

        // SO
        locationRequest = LocationRequest.create()

        getLocation()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        var isTrackingLocation: Boolean = false
        var buttonTrackLocation: Button = findViewById(R.id.button_track_location)
        locationTracker = LocationTracker(this)
        buttonTrackLocation.setOnClickListener {
            buttonTrackLocationListener(
                activity,
                locationTracker
            )
        }

    }

    private fun getLocation() {
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        val LOCATION_INTERVAL = 2000L
        locationRequest.setInterval(LOCATION_INTERVAL)
        locationRequest.setFastestInterval(LOCATION_INTERVAL)
        val fusedLocationProviderApi = LocationServices.FusedLocationApi
        val googleApiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
        if (googleApiClient != null) {
            googleApiClient.connect()
        }
    }


    fun buttonTrackLocationListener(activity: MainActivity, locationTracker: LocationTracker) {
        val hasLocationPermission: Boolean = ActivityCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPermission) {
            Toast.makeText(this.applicationContext, "permission granted", Toast.LENGTH_LONG).show()
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    // Got last known location. In some rare situations this can be null.
                    Toast.makeText(this, location?.longitude.toString(), Toast.LENGTH_LONG).show()
                }
            locationTracker.startTracking()

        } else {
            val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(
                activity,
                permissions,
                MY_PERMISSIONS_REQUEST_LOCATION
            )
            Toast.makeText(this.applicationContext, "permission denied", Toast.LENGTH_LONG).show()

        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                if (grantResults.count() > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED

                ) {
                    locationTracker.startTracking()

                } else {
                    Toast.makeText(this.applicationContext, "permission denied", Toast.LENGTH_LONG)
                        .show()

                }
            }

        }
    }

    override fun onResume() {
        super.onResume()

        mGoogleApiClient?.connect();
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }


    override fun onPause() {
        super.onPause()
        if (mGoogleApiClient?.isConnected()!!) {
            mGoogleApiClient?.disconnect();
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Timber.i("connection failed")
    }

    override fun onConnected(p0: Bundle?) {
        Timber.i("connected")
    }


    override fun onConnectionSuspended(p0: Int) {
        Timber.i("Connection suspended, please reconnect")
    }
}
