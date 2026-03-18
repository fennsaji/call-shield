package com.fenn.callshield.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenn.callshield.data.local.entity.BlocklistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistDao {
    @Query("SELECT * FROM blocklist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<BlocklistEntry>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocklist WHERE numberHash = :hash)")
    suspend fun contains(hash: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BlocklistEntry)

    @Delete
    suspend fun delete(entry: BlocklistEntry)

    @Query("DELETE FROM blocklist WHERE numberHash = :hash")
    suspend fun deleteByHash(hash: String)

    @Query("SELECT * FROM blocklist")
    suspend fun getAll(): List<BlocklistEntry>

    @Query("DELETE FROM blocklist")
    suspend fun deleteAll()

    /**
     * Bulk-delete all blocklist entries whose number has had zero calls (any outcome)
     * recorded in call_history since [cutoff]. Replaces the N+1 fetch-and-delete loop
     * in BlocklistAgingWorker with a single DELETE … WHERE NOT EXISTS query.
     */
    @Query(
        """
        DELETE FROM blocklist
        WHERE numberHash NOT IN (
            SELECT DISTINCT numberHash FROM call_history WHERE screenedAt >= :cutoff
        )
        """
    )
    suspend fun deleteEntriesWithNoCallsSince(cutoff: Long)
}
