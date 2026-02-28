package tn.dyndns.android.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.dyndns.android.models.LogEntry

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Insert
    suspend fun insert(log: LogEntry)

    @Query("DELETE FROM logs")
    suspend fun clearAll()

    @Query("DELETE FROM logs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldLogs(cutoffTime: Long)
}