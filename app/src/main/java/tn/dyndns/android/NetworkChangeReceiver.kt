package tn.dyndns.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import tn.dyndns.android.workers.DyndnsUpdateWorker

class NetworkChangeReceiver : BroadcastReceiver() {

    private var lastTriggerTime = 0L
    private var lastKnownState: Boolean? = null
    private var lastKnownNetworkInfo: String? = null
    private val DEBOUNCE_MS = 5000L
    private val TAG = "NetworkChangeReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {

            val currentTime = System.currentTimeMillis()
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork = cm.activeNetwork
            val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }

            val connected = capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            val networkInfo = getNetworkInfo(activeNetwork, capabilities)

            // Check if state actually changed
            val stateChanged = lastKnownState == null || lastKnownState != connected
            val infoChanged = lastKnownNetworkInfo == null || lastKnownNetworkInfo != networkInfo

            lastKnownState = connected
            if (networkInfo != null) lastKnownNetworkInfo = networkInfo

            // Only log on state changes or network info changes
            if (stateChanged || infoChanged) {
                val message = if (connected && networkInfo != null) {
                    "Network changed"
                } else {
                    "Network disconnected"
                }
                Log.d(TAG, message)
                logToViewModel(context, message)
            }

            // Only trigger on CONNECTED events with debounce
            if (connected && currentTime - lastTriggerTime >= DEBOUNCE_MS) {
                val message = "Network ready - triggering update ($networkInfo)"
                Log.d(TAG, message)
                logToViewModel(context, message)

                lastTriggerTime = currentTime

                val workRequest = OneTimeWorkRequestBuilder<DyndnsUpdateWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }

    private fun getNetworkInfo(
        network: android.net.Network?,
        capabilities: NetworkCapabilities?
    ): String? {
        if (network == null || capabilities == null) return null

        return buildString {
            // Determine transport type
            val transport = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) -> "WiFi Aware"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> "LoWPAN"
                else -> "Unknown"
            }

            append(
                "$transport (Network #${
                    network.toString().substringAfter("Network{").substringBefore("}")
                })"
            )

            // Add metered status
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                append(" - Unmetered")
            } else {
                append(" - Metered")
            }

            // Add validated status
            if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                append(" - Validated")
            }

            // Add bandwidth info if available
            val linkUp = capabilities.linkUpstreamBandwidthKbps
            val linkDown = capabilities.linkDownstreamBandwidthKbps
            if (linkUp > 0 || linkDown > 0) {
                append(" - ${linkDown}Kbps down / ${linkUp}Kbps up")
            }

            // For WiFi, try to extract IP if available in toString
            if (transport == "WiFi") {
                val capsString = capabilities.toString()
                val ipPattern = "IP: ([\\d.]+)".toRegex()
                val match = ipPattern.find(capsString)
                match?.groupValues?.get(1)?.let { ip ->
                    append(" - IP: $ip")
                }
            }

            // For Cellular, add subscription info
            if (transport == "Cellular") {
                val capsString = capabilities.toString()
                val subPattern = "mSubId = (\\d+)".toRegex()
                val match = subPattern.find(capsString)
                match?.groupValues?.get(1)?.let { subId ->
                    append(" - Sub ID: $subId")
                }
            }
        }
    }

    private fun logToViewModel(context: Context, message: String) {
        try {
            val viewModel = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
                .getInstance(context.applicationContext as android.app.Application)
                .create(UpdateViewModel::class.java)
            viewModel.addLog(message, "INFO")
        } catch (e: Exception) {
            Log.d(TAG, "Could not log to ViewModel: ${e.message}")
        }
    }

    companion object {
        val TAG = "NetworkChangeReceiver"
    }
}