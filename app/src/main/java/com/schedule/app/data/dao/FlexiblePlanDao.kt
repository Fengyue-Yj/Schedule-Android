package com.schedule.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.schedule.app.data.models.FlexiblePlanEntity
import com.schedule.app.data.models.FlexiblePlanWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
interface FlexiblePlanDao {
    @Query("SELECT * FROM flexible_plans WHERE termId IS NULL OR termId = :termId ORDER BY createdAt DESC")
    fun getByTermId(termId: String?): Flow<List<FlexiblePlanEntity>>

    @Query("SELECT * FROM flexible_plans WHERE termId IS NULL OR termId = :termId ORDER BY createdAt DESC")
    fun getPlansByTerm(termId: String?): Flow<List<FlexiblePlanEntity>>

    @Transaction
    @Query("SELECT * FROM flexible_plans WHERE termId IS NULL OR termId = :termId")
    fun getPlansWithSteps(termId: String?): Flow<List<FlexiblePlanWithSteps>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: FlexiblePlanEntity)

    @Update
    suspend fun update(plan: FlexiblePlanEntity)

    @Delete
    suspend fun delete(plan: FlexiblePlanEntity)

    @Query("DELETE FROM flexible_plans WHERE termId = :termId")
    suspend fun deleteByTermId(termId: String)
}
