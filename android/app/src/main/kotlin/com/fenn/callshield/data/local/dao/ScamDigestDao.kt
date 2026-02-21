package com.fenn.callshield.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenn.callshield.data.local.entity.ScamDigestEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ScamDigestDao {
    /** Most recent non-dismissed entry for the home screen card. */
    @Query("SELECT * FROM scam_digest WHERE isDismissed = 0 ORDER BY publishedAt DESC LIMIT 1")
    fun observeLatest(): Flow<ScamDigestEntry?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<ScamDigestEntry>)

    @Query("UPDATE scam_digest SET isDismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Int)

    @Query("SELECT COUNT(*) FROM scam_digest")
    suspend fun count(): Int
}
