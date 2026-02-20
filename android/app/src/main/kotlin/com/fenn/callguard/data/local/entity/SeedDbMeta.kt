package com.fenn.callguard.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Metadata about the currently loaded seed DB version. Singleton row (id = 1). */
@Entity(tableName = "seed_db_meta")
data class SeedDbMeta(
    @PrimaryKey val id: Int = 1,
    val version: Int,
    val sha256Checksum: String,
    val downloadedAt: Long = System.currentTimeMillis(),
)
