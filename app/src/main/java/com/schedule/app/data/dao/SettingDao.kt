package com.schedule.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.schedule.app.data.models.SettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings ORDER BY termStartDate DESC, createdAt DESC")
    fun getAll(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings ORDER BY termStartDate DESC, createdAt DESC")
    fun getAllSettings(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings ORDER BY termStartDate DESC, createdAt DESC")
    suspend fun getAllList(): List<SettingEntity>

    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SettingEntity?

    @Query("SELECT COUNT(*) FROM settings")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: SettingEntity)

    @Update
    suspend fun update(setting: SettingEntity)

    @Delete
    suspend fun delete(setting: SettingEntity)
}
