package com.fenn.callshield.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fenn.callshield.data.local.entity.TraiReportEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface TraiReportDao {

    @Insert
    suspend fun insert(entry: TraiReportEntry)

    @Query("SELECT * FROM trai_reports ORDER BY preparedAt DESC")
    fun observeAll(): Flow<List<TraiReportEntry>>

    @Query("SELECT COUNT(*) FROM trai_reports")
    suspend fun count(): Int

    @Query("DELETE FROM trai_reports")
    suspend fun deleteAll()
}
