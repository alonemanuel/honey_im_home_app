import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.SmsManager
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.futures.SettableFuture
import com.example.honeyhome.LocationTracker
import com.example.honeyhome.MainActivity
import com.google.common.util.concurrent.ListenableFuture

// example 2: this worker waits until it receives data from a broadcast "my_data_broadcast"
// when receiving this data, the worker finishes and passing the broadcast as a result
// it works ASYNC, meaning that the method startWork() should only return a Future.
class MyWorker(appContext: Context, workerParams: WorkerParameters, var sp: SharedPreferences) :
    ListenableWorker(appContext, workerParams) {
    private var callback: CallbackToFutureAdapter.Completer<Result>? = null
    private var receiver: BroadcastReceiver? = null

    //    private var sp: SharedPreferences? = null
    private var locationTracker: LocationTracker? = null

    @SuppressLint("RestrictedApi")
    override fun startWork(): ListenableFuture<Result> {
//        sp = applicationContext.getSharedPreferences("SP", Context.MODE_PRIVATE)
        locationTracker = LocationTracker(applicationContext as MainActivity)


        val hasLocationPermission: Boolean = ActivityCompat.checkSelfPermission(
            applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        var hasSMSPermission: Boolean = ActivityCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!(hasLocationPermission && hasSMSPermission)) {
            val future = SettableFuture.create<Result>()
            future.set(Result.success())
            return future
        }


        if (!(sp!!.getBoolean("has_home_location", false) && sp!!.getBoolean(
                "has_phone_number", false
            ))
        ) {
            val future = SettableFuture.create<Result>()
            future.set(Result.success())
            return future
        }


        // 1. here we create the future and store the callback for later use

        val future =
            CallbackToFutureAdapter.getFuture { callback: CallbackToFutureAdapter.Completer<Result> ->
                this.callback = callback
                return@getFuture null
            }

        // we place the broadcast receiver and immediately return the "future" object
        placeReceiver()
        locationTracker!!.startTracking()


        return future
    }

    // 2. we place the broadcast receiver now, waiting for it to fire in the future
    private fun placeReceiver() {
        // create the broadcast object and register it:

        this.receiver = object : BroadcastReceiver() {
            // notice that the fun onReceive() will get called in the future, not now
            override fun onReceive(context: Context?, intent: Intent?) {
                // got broadcast!
                onReceivedBroadcast(context, intent)
            }

        }

        this.getApplicationContext()
            .registerReceiver(this.receiver, IntentFilter("my_data_broadcast"))
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

    // 3. when the broadcast receiver fired, we finished the work!
    // so we will clean all and call the callback to tell WorkManager that we are DONE
    private fun onReceivedBroadcast(
        context: Context?,
        intent: Intent?
    ) {
        this.getApplicationContext().unregisterReceiver(this.receiver)


        if (locationTracker!!.accuracy < 50) {

            var sp_curr_long = sp!!.getDouble("SP_CURR_LONG", -1.0)
            var sp_curr_lat = sp!!.getDouble("SP_CURR_LAT", -1.0)

            if (!(sp_curr_long == -1.0 || sp_curr_lat == -1.0)) {
                var editor = sp!!.edit()
                editor.putDouble("SP_PREV_LONG", sp_curr_long)
                editor.putDouble("SP_PREV_LAT", sp_curr_lat)
                editor.putDouble("SP_CURR_LONG", locationTracker!!.longtitude)
                editor.putDouble("SP_CURR_LAT", locationTracker!!.latitude)
            }

            var prev_loc = Location("")
            prev_loc.setLongitude(sp!!.getDouble("SP_PREV_LONG", -1.0))
            prev_loc.setLatitude(sp!!.getDouble("SP_PREV_LAT", -1.0))
            var curr_loc = Location("")
            curr_loc.setLongitude(sp!!.getDouble("SP_CURR_LONG", -1.0))
            curr_loc.setLatitude(sp!!.getDouble("SP_CURR_LAT", -1.0))
            var home_loc = Location("")
            home_loc.setLongitude(sp!!.getDouble("SP_HOME_LONG", -1.0))
            home_loc.setLatitude(sp!!.getDouble("SP_HOME_LAT", -1.0))

            var distanceInMeters = prev_loc.distanceTo(curr_loc)

            if (distanceInMeters > 50) {
                if (curr_loc.distanceTo(home_loc) < 50) {

                    val smsManager = SmsManager.getDefault()
                    var msgArray: ArrayList<String> = smsManager.divideMessage("Honey I'm home!")
                    smsManager.sendMultipartTextMessage(
                        sp!!.getString("SP_SMS_NUMBER", null),
                        null,
                        msgArray,
                        null,
                        null
                    )
                }
            }
        }


        val callback = this.callback
        if (callback != null) {
            callback.set(Result.success())
        }
    }


}
