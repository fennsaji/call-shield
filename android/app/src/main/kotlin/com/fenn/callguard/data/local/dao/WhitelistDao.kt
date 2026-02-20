package com.fenn.callguard.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenn.callguard.data.local.entity.WhitelistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {
    @Query("SELECT * FROM whitelist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WhitelistEntry>>

    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE numberHash = :hash)")
    suspend fun contains(hash: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WhitelistEntry)

    @Delete
    suspend fun delete(entry: WhitelistEntry)

    @Query("DELETE FROM whitelist WHERE numberHash = :hash")
    suspend fun deleteByHash(hash: String)

    @Query("DELETE FROM whitelist")
    suspend fun deleteAll()
}
