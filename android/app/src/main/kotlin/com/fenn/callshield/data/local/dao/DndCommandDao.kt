package com.fenn.callshield.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fenn.callshield.data.local.entity.DndCommandEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DndCommandDao {

    @Insert
    suspend fun insert(entry: DndCommandEntry)

    /** Latest non-STATUS command — used to derive the current DND status card. */
    @Query("SELECT * FROM dnd_commands WHERE command != 'STATUS' ORDER BY sentAt DESC LIMIT 1")
    fun observeLatestActive(): Flow<DndCommandEntry?>

    /** All commands in reverse-chronological order, for a history view if needed. */
    @Query("SELECT * FROM dnd_commands ORDER BY sentAt DESC")
    fun observeAll(): Flow<List<DndCommandEntry>>

    @Query("UPDATE dnd_commands SET confirmedByUser = 1, confirmedAt = :ts WHERE id = :id")
    suspend fun markConfirmed(id: Long, ts: Long = System.currentTimeMillis())
}
