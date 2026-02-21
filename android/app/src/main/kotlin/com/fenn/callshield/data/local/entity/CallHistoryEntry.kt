package com.fenn.callshield.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local call history — populated by the screening service for every screened call.
 * Stores only the hash, never the raw number.
 *
 * [outcome]         "allowed" | "silenced" | "rejected" | "flagged"
 * [decisionSource]  name of [DecisionSource] enum — e.g. "SEED_DB", "REMOTE", "BLOCKLIST"
 *                   Used by the Reason Transparency Panel (PRD §3.14).
 */
@Entity(tableName = "call_history")
data class CallHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numberHash: String,
    val displayLabel: String,
    val outcome: String,
    val confidenceScore: Double,
    val category: String?,
    val decisionSource: String,
    val screenedAt: Long = System.currentTimeMillis(),
)
