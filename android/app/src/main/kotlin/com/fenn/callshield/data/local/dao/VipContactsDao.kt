package com.fenn.callshield.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fenn.callshield.data.local.entity.VipContactEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface VipContactsDao {
    @Query("SELECT * FROM vip_contacts ORDER BY displayLabel ASC")
    fun observeAll(): Flow<List<VipContactEntry>>

    @Query("SELECT * FROM vip_contacts ORDER BY displayLabel ASC")
    suspend fun getAll(): List<VipContactEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VipContactEntry)

    @Query("DELETE FROM vip_contacts WHERE numberHash = :numberHash")
    suspend fun deleteByHash(numberHash: String)

    @Query("DELETE FROM vip_contacts")
    suspend fun deleteAll()
}
