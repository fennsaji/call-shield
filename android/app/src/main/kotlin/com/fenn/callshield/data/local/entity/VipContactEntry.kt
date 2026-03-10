package com.fenn.callshield.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vip_contacts")
data class VipContactEntry(
    /** HMAC-SHA256 hash of the E.164 number. Raw number is never stored. */
    @PrimaryKey val numberHash: String,
    /** Display name sourced from the device contacts at time of adding. */
    val displayLabel: String,
    val addedAt: Long = System.currentTimeMillis(),
)
