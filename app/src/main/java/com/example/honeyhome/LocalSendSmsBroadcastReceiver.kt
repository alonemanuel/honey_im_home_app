package com.example.honeyhome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import timber.log.Timber

class LocalSendSmsBroadcastReceiver(var activity: MainActivity) : BroadcastReceiver() {
    var NUMBER = ""
    var CONTENT = ""


    override fun onReceive(context: Context?, intent: Intent?) {
        // check for permissions
        Toast.makeText(activity, "recieved sms broadcast", Toast.LENGTH_LONG).show()
        val hasSMSPermission: Boolean = ActivityCompat.checkSelfPermission(
            activity, android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSMSPermission) {
            NUMBER = intent!!.getStringExtra("sp_number")
            CONTENT = intent!!.getStringExtra("sp_message")
            val smsManager = SmsManager.getDefault()
            Toast.makeText(activity, NUMBER, Toast.LENGTH_SHORT).show()
            smsManager.sendTextMessage(NUMBER, null, CONTENT, null, null)
        } else {
            Timber.e("No permissions for sending sms")

        }
    }
}