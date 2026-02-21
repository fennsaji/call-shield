package com.fenn.callshield.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fenn.callshield.data.local.entity.CallerEventEntry

@Dao
interface CallerEventDao {

    @Insert
    suspend fun insert(event: CallerEventEntry)

    /** Count events of any type for [hash] within the given time window. */
    @Query(
        """
        SELECT COUNT(*) FROM recent_caller_events
        WHERE numberHash = :hash AND occurredAt >= :since
        """
    )
    suspend fun countSince(hash: String, since: Long): Int

    /** Count SHORT_RING events for [hash] within the given time window. */
    @Query(
        """
        SELECT COUNT(*) FROM recent_caller_events
        WHERE numberHash = :hash AND eventType = 'SHORT_RING' AND occurredAt >= :since
        """
    )
    suspend fun countShortRingsSince(hash: String, since: Long): Int

    /** Delete all rows older than [cutoff] — enforces 24h TTL. */
    @Query("DELETE FROM recent_caller_events WHERE occurredAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    /**
     * Enforce 100-row cap per hash: delete the oldest rows beyond position 100.
     * Uses OFFSET 100 on a DESC-ordered sub-select.
     */
    @Query(
        """
        DELETE FROM recent_caller_events
        WHERE id IN (
            SELECT id FROM recent_caller_events
            WHERE numberHash = :hash
            ORDER BY occurredAt DESC
            LIMIT -1 OFFSET 100
        )
        """
    )
    suspend fun enforceCapForHash(hash: String)
}
