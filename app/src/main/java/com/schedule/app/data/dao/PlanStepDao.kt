package com.schedule.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.schedule.app.data.models.PlanStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanStepDao {
    @Query("SELECT * FROM plan_steps WHERE planId = :planId ORDER BY position ASC")
    fun getByPlanId(planId: String): Flow<List<PlanStepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: PlanStepEntity)

    @Update
    suspend fun update(step: PlanStepEntity)

    @Delete
    suspend fun delete(step: PlanStepEntity)
}
