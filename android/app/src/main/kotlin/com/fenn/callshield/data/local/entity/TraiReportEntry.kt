package com.fenn.callshield.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records a TRAI DND complaint that was prepared by the user.
 *
 * Status is always "Complaint prepared" — the app cannot verify whether
 * the SMS was actually sent. PRD §7 constraint: must not imply the complaint
 * reached TRAI; the user sends it manually from the SMS app.
 */
@Entity(tableName = "trai_reports")
data class TraiReportEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numberHash: String,
    val displayLabel: String,  // masked, e.g. "****1234"
    val preparedAt: Long = System.currentTimeMillis(),
)
