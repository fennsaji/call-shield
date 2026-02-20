package com.fenn.callshield.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single number-hash entry from the seed database CSV.
 * Loaded in bulk via WorkManager during the seed DB update job.
 */
@Entity(tableName = "seed_db_numbers")
data class SeedDbNumber(
    @PrimaryKey val numberHash: String,
    val category: String,
    val confidenceScore: Double,
)
