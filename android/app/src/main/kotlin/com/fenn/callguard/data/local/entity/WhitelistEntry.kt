package com.fenn.callguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist")
data class WhitelistEntry(
    @PrimaryKey val numberHash: String,
    val displayLabel: String,
    val addedAt: Long = System.currentTimeMillis(),
)
