package tn.dyndns.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tn.dyndns.android.database.AppDatabase
import tn.dyndns.android.models.LogEntry

class UpdateViewModel : ViewModel() {

    private val db = AppDatabase.getDatabase(DyndnsAndroid.instance)
    val logs: LiveData<List<LogEntry>> = db.logDao().getAllLogs().asLiveData()

    private val _updateSummary = MutableLiveData<UpdateSummary>()
    val updateSummary: LiveData<UpdateSummary> = _updateSummary

    fun addLog(message: String, level: String = "DEBUG") {
        viewModelScope.launch {
            val logEntry = LogEntry(message = message, level = level)
            db.logDao().insert(logEntry)

            val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            db.logDao().deleteOldLogs(weekAgo)
        }
    }

    fun postUpdate(summary: String, results: List<String>, timestamp: Long) {
        _updateSummary.postValue(UpdateSummary(summary, results, timestamp))
    }

    fun clearLogs() {
        viewModelScope.launch {
            db.logDao().clearAll()
        }
    }

    data class UpdateSummary(
        val summary: String,
        val results: List<String>,
        val timestamp: Long
    )
}