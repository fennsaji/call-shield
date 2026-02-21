package com.fenn.callshield.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records a behavioral event for a caller hash.
 *
 * [eventType]   "INCOMING_CALL" | "SHORT_RING"
 * [occurredAt]  epoch millis
 *
 * 24h hard TTL enforced by [BehavioralPurgeWorker].
 * 100-row cap per hash enforced by [CallerEventDao.enforceCapForHash].
 */
@Entity(
    tableName = "recent_caller_events",
    indices = [Index(value = ["numberHash", "occurredAt"])],
)
data class CallerEventEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numberHash: String,
    val eventType: String,
    val occurredAt: Long = System.currentTimeMillis(),
)
