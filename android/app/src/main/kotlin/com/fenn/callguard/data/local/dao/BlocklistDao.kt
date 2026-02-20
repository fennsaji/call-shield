package com.fenn.callguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenn.callguard.data.local.entity.BlocklistEntry
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

    @Query("DELETE FROM blocklist")
    suspend fun deleteAll()
}
