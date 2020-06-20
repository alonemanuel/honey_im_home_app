import android.content.*
import android.content.pm.PackageManager
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture

// example 2: this worker waits until it receives data from a broadcast "my_data_broadcast"
// when receiving this data, the worker finishes and passing the broadcast as a result
// it works ASYNC, meaning that the method startWork() should only return a Future.
class MyWorker(appContext: Context, workerParams: WorkerParameters) :
    ListenableWorker(appContext, workerParams) {
    private var callback: CallbackToFutureAdapter.Completer<Result>? = null
    private var receiver: BroadcastReceiver? = null
    private var sp: SharedPreferences = null

    override fun startWork(): ListenableFuture<Result> {

        val hasLocationPermission: Boolean = ActivityCompat.checkSelfPermission(
            applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        var hasSMSPermission: Boolean = ActivityCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!(hasLocationPermission && hasSMSPermission)) {
            
        }

        // 1. here we create the future and store the callback for later use


        val future =
            CallbackToFutureAdapter.getFuture { callback: CallbackToFutureAdapter.Completer<Result> ->
                this.callback = callback
                return@getFuture null
            }

        // we place the broadcast receiver and immediately return the "future" object
        placeReceiver()
        return future
    }

    // 2. we place the broadcast receiver now, waiting for it to fire in the future
    private fun placeReceiver() {
        // create the broadcast object and register it:

        this.receiver = object : BroadcastReceiver() {
            // notice that the fun onReceive() will get called in the future, not now
            override fun onReceive(context: Context?, intent: Intent?) {
                // got broadcast!
                onReceivedBroadcast()
            }
        }

        this.getApplicationContext()
            .registerReceiver(this.receiver, IntentFilter("my_data_broadcast"))
    }

    // 3. when the broadcast receiver fired, we finished the work!
    // so we will clean all and call the callback to tell WorkManager that we are DONE
    private fun onReceivedBroadcast() {
        this.getApplicationContext().unregisterReceiver(this.receiver)

        val callback = this.callback
        if (callback != null) {
            callback.set(Result.success())
        }
    }


}
