package com.fenn.callshield.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fenn.callshield.data.local.entity.SeedDbMeta
import com.fenn.callshield.data.local.entity.SeedDbNumber

@Dao
abstract class SeedDbDao {
    // ---- Meta ----
    @Query("SELECT * FROM seed_db_meta WHERE id = 1")
    abstract suspend fun getMeta(): SeedDbMeta?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertMeta(meta: SeedDbMeta)

    // ---- Numbers ----
    @Query("SELECT * FROM seed_db_numbers WHERE numberHash = :hash")
    abstract suspend fun lookup(hash: String): SeedDbNumber?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(numbers: List<SeedDbNumber>)

    @Query("DELETE FROM seed_db_numbers")
    abstract suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM seed_db_numbers")
    abstract suspend fun count(): Int

    /**
     * Atomically replace all seed DB entries. Runs clearAll + insertAll in a
     * single transaction so a checksum failure during download never wipes
     * previously valid data — the old data stays intact until a verified
     * replacement is ready.
     */
    @Transaction
    open suspend fun replaceAll(numbers: List<SeedDbNumber>) {
        clearAll()
        if (numbers.isNotEmpty()) insertAll(numbers)
    }
}
