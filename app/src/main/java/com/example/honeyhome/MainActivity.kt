package com.example.honeyhome

import MyWorker
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.honeyhome.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.util.concurrent.TimeUnit


class MainActivity : GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, AppCompatActivity() {
    lateinit var locationTracker: LocationTracker
    lateinit var binding: ActivityMainBinding
    lateinit var sp: SharedPreferences
    lateinit var editor: SharedPreferences.Editor

    val myReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onReceiveBroadcast(context, intent)
        }
    }

    val smsReceiver = LocalSendSmsBroadcastReceiver(this)

    fun onReceiveBroadcast(context: Context?, intent: Intent?) {
        if (locationTracker.isTracking) {
            binding.apply {

                buttonTrackLocation.text = "Stop Tracking Location"

                longtitudeTextView.text = locationTracker.longtitude.toString()
                latitudeTextView.text = locationTracker.latitude.toString()
                accuracyTextView.text = locationTracker.accuracy.toString()

                if (locationTracker.accuracy < 50) {
                    setHomeLocationButton.visibility = View.VISIBLE
                } else {
                    setHomeLocationButton.visibility = View.INVISIBLE
                }
            }
        } else {
            binding.apply {
                buttonTrackLocation.text = "Start Tracking Location"
                longtitudeTextView.text = "-"
                latitudeTextView.text = "-"
                accuracyTextView.text = "-"
                setHomeLocationButton.visibility = View.INVISIBLE
                if (locationTracker.hasHomeLocation) {
                    clearHomeButton.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree())
        Timber.i("In onCreate()")
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        editor = sp.edit()

        registerReceiver(myReceiver, IntentFilter("start_tracking"))
        registerReceiver(smsReceiver, IntentFilter("POST_PC.ACTION_SEND_SMS"))

        initSharedPref()

        val storedLatitude = sp.getDouble(getString(R.string.SP_LATITUDE), 0.0)
        val storedLongtitude = sp.getDouble(getString(R.string.SP_LONGTITUDE), 0.0)
        val storedHomeSetFlag = sp.getBoolean(getString(R.string.SP_HOMEFLAG), false)

        locationTracker = LocationTracker(this)
        if (savedInstanceState != null && savedInstanceState.keySet()
                .contains("SP_TRACKING")
        ) {
            if (savedInstanceState.getBoolean("SP_TRACKING"))
                locationTracker.startTracking()

        }


        if (storedHomeSetFlag) {
            locationTracker.setHomeLocation(storedLatitude, storedLongtitude)
            binding.homeLocationGeoView.text =
                "(${locationTracker.homeLatitude}[lat], ${locationTracker.homeLongtitude}[long])"
            binding.clearHomeButton.visibility = View.VISIBLE

        }


        binding.apply {
            buttonTrackLocation.setOnClickListener { buttonTrackLocationListener() }
            setHomeLocationButton.setOnClickListener { setHomeLocationListener() }
            clearHomeButton.setOnClickListener { clearHomeLocationButtonListener() }
            setSmsButton.setOnClickListener { setSmsButtonListener() }
            testSmsButton.setOnClickListener { testSmsButtonListener() }
        }

    }

    fun initSharedPref() {
        if (sp.contains(getString(R.string.SP_SMS_NUMBER))) {
            val smsNumber = sp.getString(getString(R.string.SP_SMS_NUMBER), "0")
            binding.apply {
                phone_label.text = smsNumber
                testSmsButton.visibility = View.VISIBLE
            }
        }

    }

    fun testSmsButtonListener() {
        val intent = Intent()

        intent.action = "POST_PC.ACTION_SEND_SMS"
        val number = sp.getString("SP_SMS_NUMBER", "0")
        val message = "Honey I'm Sending a Test Message!"
        intent.putExtra("sp_number", number)
        intent.putExtra("sp_message", message)
        this.sendBroadcast(intent)
    }


    fun getPermissionAndSetSMS(): Boolean {
        val hasSMSPermission: Boolean = ActivityCompat.checkSelfPermission(
            this, android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        when {
            hasSMSPermission -> {
                return true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.SEND_SMS
            ) -> {
                begForSMSPermissions()
                return false
            }
            else -> {

                requestSMSPermissionsWrapper()
                return false
            }

        }
    }

    fun requestSMSPermissionsWrapper() {
        val permissions = arrayOf(Manifest.permission.SEND_SMS)
        ActivityCompat.requestPermissions(
            this,
            permissions,
            100
        )
    }

    fun begForSMSPermissions() {

        val builder1 =
            AlertDialog.Builder(this)
        builder1.setMessage("We need your permission to let us send an SMS. We promise we won't do anything else with it!")
        builder1.setCancelable(true)

        builder1.setPositiveButton(
            "OK"
        ) { dialog, id -> requestSMSPermissionsWrapper() }

        builder1.setNegativeButton(
            "NO THANKS"
        ) { dialog, id -> dialog.cancel() }

        val alert11 = builder1.create()
        alert11.show()
    }

    fun setSmsButtonListener() {
        if (getPermissionAndSetSMS()) {
            showSetNumberDialog()
        }

    }

    fun showSetNumberDialog() {
        val alert: AlertDialog.Builder = AlertDialog.Builder(this)

        alert.setTitle("Enter your phone number")
        alert.setMessage("phone number:")

// Set an EditText view to get user input

// Set an EditText view to get user input
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_PHONE
        alert.setView(input)

        alert.setPositiveButton("Ok",
            DialogInterface.OnClickListener { dialog, whichButton ->
                val value: String = input.text.toString()
                editor.putString(getString(R.string.SP_SMS_NUMBER), value)
                editor.apply()
                binding.testSmsButton.visibility = View.VISIBLE
                binding.phoneLabel.text = value
                // Do something with value!

            })

        alert.setNegativeButton("Cancel",
            DialogInterface.OnClickListener { dialog, whichButton ->
                // Canceled.
            })

        alert.show()
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

        editor.remove(getString(R.string.SP_LONGTITUDE))
        editor.remove(getString(R.string.SP_LATITUDE))
        editor.remove(getString(R.string.SP_HOMEFLAG))
        editor.apply()

        binding.homeLocationGeoView.text = "Home location not set"
        binding.clearHomeButton.visibility = View.INVISIBLE
    }

    fun setHomeLocationListener() {
        locationTracker.setHomeLocation(locationTracker.latitude, locationTracker.longtitude)
        editor.putDouble(getString(R.string.SP_LATITUDE), locationTracker.latitude)
        editor.putDouble(getString(R.string.SP_LONGTITUDE), locationTracker.longtitude)
        editor.putBoolean(getString(R.string.SP_HOMEFLAG), true)
        editor.apply()

        binding.clearHomeButton.visibility = View.VISIBLE
        binding.homeLocationGeoView.text =
            "(${locationTracker.homeLatitude}[lat], ${locationTracker.homeLongtitude}[long])"

    }

    fun buttonTrackLocationListener() {
        var trackingButton = binding.buttonTrackLocation
        if (locationTracker.isTracking) {
            locationTracker.stopTracking()
            trackingButton.text = "Start Tracking Location"
        } else {

            var periodicRequest =
                PeriodicWorkRequest.Builder(MyWorker::class.java, 15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.NONE)
                    .build()
            val workManager = WorkManager.getInstance(this)
            workManager.enqueueUniquePeriodicWork(
                "WORK_LOCATION",
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicRequest
            )


            locationTracker.startTracking()
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        locationTracker.stopTracking()
        unregisterReceiver(myReceiver)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
            when (requestCode) {
                99 -> {
                    locationTracker.startTracking()
                }
                100 -> {
                    showSetNumberDialog()
                }
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
        }


    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putDouble(getString(R.string.SP_LATITUDE), locationTracker.latitude)
        outState.putDouble(getString(R.string.SP_LONGTITUDE), locationTracker.longtitude)
        outState.putFloat("SP_ACCURACY", locationTracker.accuracy)
        outState.putBoolean("SP_TRACKING", locationTracker.isTracking)
        super.onSaveInstanceState(outState)
    }


}
