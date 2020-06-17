package com.example.honeyhome

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import timber.log.Timber

class LocalSendSmsBroadcastReceiver(var activity: MainActivity) : BroadcastReceiver() {
    var NUMBER = ""
    var CONTENT = ""


    override fun onReceive(context: Context?, intent: Intent?) {
        // check for permissions
//        Toast.makeText(activity, "recieved sms broadcast", Toast.LENGTH_LONG).show()
        val hasSMSPermission: Boolean = ActivityCompat.checkSelfPermission(
            activity, android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSMSPermission) {
            NUMBER = intent!!.getStringExtra("sp_number")
            CONTENT = intent!!.getStringExtra("sp_message")
            val smsManager = SmsManager.getDefault()
            var msgArray: ArrayList<String> = smsManager.divideMessage(CONTENT)
            smsManager.sendMultipartTextMessage(NUMBER, null, msgArray, null, null)


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel
                val name = "smsSentNotification"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val mChannel = NotificationChannel("73", name, importance)
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                val notificationManager =
                    context!!.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(mChannel)
            }


            var ntfc: Notification =
                NotificationCompat.Builder(context!!, "73")
                    .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                    .setContentTitle("bla bla notif")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
            var notifId = 5364
            NotificationManagerCompat.from(context).notify(notifId, ntfc)

//            smsManager.sendTextMessage(NUMBER, null, CONTENT, null, null)
        } else {
            Timber.e("No permissions for sending sms")

        }
    }


}