package tn.dyndns.android

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log

class DyndnsAndroid : Application() {

    companion object {
        val TAG = "DyndnsAndroid"
        lateinit var instance: DyndnsAndroid
            private set
    }

    private val networkChangeReceiver = NetworkChangeReceiver()

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Register network receiver
        registerNetworkReceiver()

        // Ensure periodic work is scheduled on app launch

        // Schedule periodic worker (to make sure it's scheduled when app is opened)
        schedulePeriodicWorker(this)
    }

    private fun registerNetworkReceiver() {
        try {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiver(networkChangeReceiver, filter)
            Log.d(TAG, "Network receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receiver", e)
        }
    }

    private fun schedulePeriodicWorker(context: Context) {
        // Reuse the same method from BootReceiver
        BootReceiver().schedulePeriodic(context)
    }
}