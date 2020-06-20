package com.example.honeyhome

import android.app.Application
import android.content.IntentFilter

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        var app = applicationContext as MainActivity
        val smsReceiver = LocalSendSmsBroadcastReceiver(app)
        registerReceiver(smsReceiver, IntentFilter("POST_PC.ACTION_SEND_SMS"))

    }
}