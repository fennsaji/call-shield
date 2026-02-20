package com.fenn.callguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocklist")
data class BlocklistEntry(
    @PrimaryKey val numberHash: String,
    /** Display-safe label — e.g. last 4 digits or user alias. Never full number. */
    val displayLabel: String,
    val addedAt: Long = System.currentTimeMillis(),
)
