package tn.dyndns.android.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import tn.dyndns.android.database.AppDatabase
import tn.dyndns.android.models.*
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class DyndnsUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val entries = db.dyndnsDao().getEnabledEntries()

                if (entries.isEmpty()) {
                    log("No enabled entries to update", "INFO")
                    return@withContext Result.success()
                }

                var success = 0
                entries.forEach { entry ->
                    val result = updateEntry(entry, db)
                    if (result) success++
                }

                log("Update completed: $success/${entries.size}", "INFO")
                Result.success()
            } catch (e: Exception) {
                log("Work failed: ${e.message}", "ERROR")
                Result.retry()
            }
        }
    }

    private suspend fun updateEntry(entry: DyndnsEntry, db: AppDatabase): Boolean {
        return try {
            val domain = entry.fqdn ?: entry.hostname
            val resolvedIp = resolveDns(domain)

            val request = buildRequest(entry)
            log("Executing request for ${entry.name}", "INFO")

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val isSuccessful = response.isSuccessful

            val updated = entry.copy(
                lastUpdateTime = System.currentTimeMillis(),
                lastStatus = if (isSuccessful) "OK" else "Failed",
                resolvedIp = resolvedIp
            )
            db.dyndnsDao().update(updated)

            log("${entry.name}: ${if (isSuccessful) "OK" else "Failed"} (DNS: $resolvedIp) Response: $responseBody",
                if (isSuccessful) "INFO" else "ERROR")

            isSuccessful
        } catch (e: Exception) {
            handleError(entry, e, db)
            false
        }
    }

    private fun buildRequest(entry: DyndnsEntry): Request {
        return when (entry.providerType) {
            ProviderType.DUCKDNS -> {
                val config = ConfigConverter.fromJson<DuckDnsConfig>(entry.providerConfig)
                val url = "https://www.duckdns.org/update?domains=${entry.hostname}&token=${config.token}&ip="
                Request.Builder().url(url).get().build()
            }
            ProviderType.DYNU -> {
                val config = ConfigConverter.fromJson<DynuConfig>(entry.providerConfig)
                val url = "https://api.dynu.com/nic/update?hostname=${entry.fqdn ?: entry.hostname}"
                val credentials = Credentials.basic(config.username, config.password)
                Request.Builder().url(url)
                    .header("Authorization", credentials)
                    .get()
                    .build()
            }
            ProviderType.FREEFORM -> {
                val config = ConfigConverter.fromJson<FreeformConfig>(entry.providerConfig)
                val requestBuilder = Request.Builder().url(config.url)

                // Add headers
                config.headers.forEach { (key, value) ->
                    requestBuilder.header(key, value)
                }

                // Add authentication
                if (config.authType == AuthType.BASIC) {
                    val credentials = Credentials.basic(config.username ?: "", config.password ?: "")
                    requestBuilder.header("Authorization", credentials)
                }

                // Set method and body
                when (config.method) {
                    "POST" -> {
                        val body = config.body?.let {
                            RequestBody.create(null, it)
                        } ?: RequestBody.create(null, "")
                        requestBuilder.post(body)
                    }
                    else -> requestBuilder.get()
                }

                requestBuilder.build()
            }
        }
    }

    private suspend fun handleError(entry: DyndnsEntry, e: Exception, db: AppDatabase) {
        val resolvedIp = try {
            resolveDns(entry.fqdn ?: entry.hostname)
        } catch (_: Exception) {
            "DNS lookup failed"
        }

        val updated = entry.copy(
            lastUpdateTime = System.currentTimeMillis(),
            lastStatus = "Error",
            resolvedIp = resolvedIp
        )
        db.dyndnsDao().update(updated)

        log("${entry.name}: Error - ${e.message}", "ERROR")
    }

    private fun resolveDns(domain: String): String {
        return try {
            InetAddress.getAllByName(domain)
                .filter { it.hostAddress != null }
                .joinToString(", ") { it.hostAddress }
        } catch (e: Exception) {
            "DNS lookup failed"
        }
    }

    private suspend fun log(message: String, level: String) {
        Log.d(TAG, "[$level] $message")
        val db = AppDatabase.getDatabase(applicationContext)
        db.logDao().insert(LogEntry(message = message, level = level))
    }

    companion object {
        val TAG = "DyndnsUpdateWorker"
        fun triggerImmediate(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<DyndnsUpdateWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}