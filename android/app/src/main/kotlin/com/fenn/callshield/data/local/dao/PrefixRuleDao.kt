package com.fenn.callshield.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenn.callshield.data.local.entity.PrefixRule
import kotlinx.coroutines.flow.Flow

@Dao
interface PrefixRuleDao {
    @Query("SELECT * FROM prefix_rules ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<PrefixRule>>

    /** Returns all rules sorted longest-pattern-first for priority matching. */
    @Query("SELECT * FROM prefix_rules ORDER BY LENGTH(pattern) DESC")
    suspend fun getAllSortedByLength(): List<PrefixRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: PrefixRule)

    @Query("DELETE FROM prefix_rules WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM prefix_rules")
    suspend fun deleteAll()
}
