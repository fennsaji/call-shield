package com.fenn.callshield.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Prefix rule for call screening.
 * A prefix is an E.164 prefix string, e.g. "+91140" for telemarketer prefix.
 * [action] is "block" or "allow".
 */
@Entity(tableName = "prefix_rules")
data class PrefixRule(
    @PrimaryKey val prefix: String,
    val action: String, // "block" | "allow"
    val label: String = "",
    val addedAt: Long = System.currentTimeMillis(),
)
