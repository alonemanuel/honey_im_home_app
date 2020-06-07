package com.example.honeyhome

import android.content.*
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.example.honeyhome.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import timber.log.Timber


class MainActivity : GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, AppCompatActivity() {
    lateinit var locationTracker: LocationTracker
    private lateinit var locationCallback: LocationCallback
    lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mGoogleApiClient: GoogleApiClient
    lateinit var binding: ActivityMainBinding

    val myReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            binding.longtitudeTextView.text = locationTracker.longtitude.toString()
            binding.latitudeTextView.text = locationTracker.latitude.toString()
            binding.accuracyTextView.text = locationTracker.accuracy.toString()

            if (locationTracker.accuracy < 50) {
                binding.setHomeLocationButton.visibility = View.VISIBLE
            } else {
                binding.setHomeLocationButton.visibility = View.GONE
            }
            Timber.i(
                "got intent with data=%s", (intent?.action.toString())
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        Timber.i("In onCreate()")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val activity: MainActivity = this
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
        registerReceiver(myReceiver, IntentFilter("start_tracking"))

        val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val storedLatitude = sp.getDouble("SP_LATITUDE", 0.0)
        val storedLongtitude = sp.getDouble("SP_LONGTITUDE", 0.0)
        val editor: SharedPreferences.Editor=sp.edit()
//        editor.putDouble()
        editor.apply()


//        locationRequest = LocationRequest.create()
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        mGoogleApiClient = GoogleApiClient.Builder(this)
//            .addConnectionCallbacks(this)
//            .addOnConnectionFailedListener(this)
//            .addApi(LocationServices.API)
//            .build()
//        getLocation()


        locationTracker = LocationTracker(this)
        binding.apply {
            buttonTrackLocation.setOnClickListener { buttonTrackLocationListener() }
            setHomeLocationButton.setOnClickListener { setHomeLocationListener() }
            clearHomeButton.setOnClickListener { clearHomeLocationButtonListener() }
        }

    }

    fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
        putLong(key, java.lang.Double.doubleToRawLongBits(double))

    fun SharedPreferences.getDouble(key: String, default: Double) =
        java.lang.Double.longBitsToDouble(
            getLong(
                key,
                java.lang.Double.doubleToRawLongBits(default)
            )
        )

    fun clearHomeLocationButtonListener() {
        locationTracker.clearHomeLocation()
        binding.homeLocationGeoView.text = "Home location not set"
        binding.clearHomeButton.visibility = View.GONE
    }

    fun setHomeLocationListener() {
        locationTracker.setLocationAsHome()
        binding.homeLocationGeoView.text =
            "(${locationTracker.homeLatitude}[lat], ${locationTracker.homeLongtitude}[long])"

    }

//    private fun getLocation() {
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//        val LOCATION_INTERVAL = 2000L
//        locationRequest.setInterval(LOCATION_INTERVAL)
//        locationRequest.setFastestInterval(LOCATION_INTERVAL)
//
//        // removed null check
//        mGoogleApiClient.connect()
//    }


    fun buttonTrackLocationListener() {
        var trackingButton = findViewById<Button>(R.id.button_track_location)
        if (locationTracker.isTracking) {
            locationTracker.stopTracking()
            trackingButton.text = "Start Tracking Location"
        } else {
            locationTracker.startTracking()
            trackingButton.text = "Stop Tracking Location"
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(myReceiver)
    }

    override fun onResume() {
        super.onResume()
//
//        mGoogleApiClient?.connect();
//        fusedLocationClient.requestLocationUpdates(
//            locationRequest,
//            locationCallback,
//            Looper.getMainLooper()
//        )
    }


    override fun onPause() {
        super.onPause()
//        if (mGoogleApiClient?.isConnected()!!) {
//            mGoogleApiClient?.disconnect();
//        }
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
