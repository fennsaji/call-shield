package com.fenn.callguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenn.callguard.data.local.entity.SeedDbMeta
import com.fenn.callguard.data.local.entity.SeedDbNumber

@Dao
interface SeedDbDao {
    // ---- Meta ----
    @Query("SELECT * FROM seed_db_meta WHERE id = 1")
    suspend fun getMeta(): SeedDbMeta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeta(meta: SeedDbMeta)

    // ---- Numbers ----
    @Query("SELECT * FROM seed_db_numbers WHERE numberHash = :hash")
    suspend fun lookup(hash: String): SeedDbNumber?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(numbers: List<SeedDbNumber>)

    @Query("DELETE FROM seed_db_numbers")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM seed_db_numbers")
    suspend fun count(): Int
}
