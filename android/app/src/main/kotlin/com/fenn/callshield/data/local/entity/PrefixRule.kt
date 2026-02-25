package com.fenn.callshield.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A pattern rule for call screening.
 *
 * [pattern] is matched against the incoming E.164 number using [matchType]:
 *   "prefix"   → number starts with pattern (e.g. "+91140")
 *   "suffix"   → number ends with pattern   (e.g. "9999")
 *   "contains" → number contains pattern    (e.g. "140")
 *
 * [action] is "block", "silence", or "allow".
 */
@Entity(tableName = "prefix_rules")
data class PrefixRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pattern: String,
    val matchType: String = "prefix", // "prefix" | "suffix" | "contains"
    val action: String,               // "block"  | "silence" | "allow"
    val label: String = "",
    val addedAt: Long = System.currentTimeMillis(),
)
