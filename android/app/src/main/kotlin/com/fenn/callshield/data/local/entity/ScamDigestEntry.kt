package com.fenn.callshield.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Weekly scam digest card shown on Home screen.
 * Phase 1: manually curated, seeded from assets/scam_digest.json on first launch.
 * PRD §3.10
 */
@Entity(tableName = "scam_digest")
data class ScamDigestEntry(
    @PrimaryKey val id: Int,
    val title: String,
    val body: String,
    val source: String,
    val publishedAt: Long,
    val isDismissed: Boolean = false,
)
