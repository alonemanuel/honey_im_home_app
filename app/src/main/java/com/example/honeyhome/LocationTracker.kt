package com.example.honeyhome

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat

class LocationTracker(context: Context) {
    val longtitude: Int = 0
    val altitude: Int = 0
    val accuracy: Int = 0
    val contex: Context = context

    init {

    }

    fun startTracking() {

        if (ActivityCompat.checkSelfPermission(
                contex,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        ) {
            // track
            Toast.makeText(contex, "Starting to track", Toast.LENGTH_LONG).show()

        } else {
            // log error
            Toast.makeText(contex, "No permission erorrrorror", Toast.LENGTH_LONG).show()
        }
    }

    fun stopTracking() {

    }

}