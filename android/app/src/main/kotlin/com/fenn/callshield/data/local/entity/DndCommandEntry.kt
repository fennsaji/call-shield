package com.fenn.callshield.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records a DND command the user has prepared and sent via SMS to 1909.
 *
 * [command]  "FULL" | "PARTIAL" | "DEACTIVATE" | "STATUS"
 * [smsBody]  Exact text sent: "START 0", "START 1,4", "STOP", "STATUS"
 * [categories] Comma-separated TRAI category codes for PARTIAL mode, null otherwise.
 *
 * [confirmedByUser] The app cannot read SMS replies (READ_SMS violates privacy policy).
 * Instead the user manually taps "Mark as Confirmed" after receiving TRAI's reply.
 */
@Entity(tableName = "dnd_commands")
data class DndCommandEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val smsBody: String,
    val categories: String?,
    val sentAt: Long = System.currentTimeMillis(),
    val confirmedByUser: Boolean = false,
    val confirmedAt: Long? = null,
)
