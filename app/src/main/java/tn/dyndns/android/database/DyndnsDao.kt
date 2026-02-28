package tn.dyndns.android.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import tn.dyndns.android.models.DyndnsEntry

@Dao
interface DyndnsDao {
    @Query("SELECT * FROM dyndns_entries ORDER BY name")
    fun getAllEntries(): Flow<List<DyndnsEntry>>

    @Query("SELECT * FROM dyndns_entries WHERE enabled = 1")
    suspend fun getEnabledEntries(): List<DyndnsEntry>

    @Insert
    suspend fun insert(entry: DyndnsEntry)

    @Update
    suspend fun update(entry: DyndnsEntry)

    @Delete
    suspend fun delete(entry: DyndnsEntry)

    @Query("SELECT * FROM dyndns_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): DyndnsEntry?
}