package com.schedule.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.schedule.app.data.models.AssignmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE termId = :termId ORDER BY dueDate ASC")
    fun getByTermId(termId: String?): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE termId = :termId ORDER BY dueDate ASC")
    fun getAssignmentsByTerm(termId: String?): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE termId = :termId ORDER BY dueDate ASC")
    fun getAssignmentsForTerm(termId: String?): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE sourceID = :sourceId LIMIT 1")
    suspend fun findBySourceId(sourceId: String): AssignmentEntity?

    @Query("SELECT * FROM assignments WHERE sourceID = :sourceId LIMIT 1")
    suspend fun getBySourceId(sourceId: String): AssignmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assignment: AssignmentEntity)

    @Update
    suspend fun update(assignment: AssignmentEntity)

    @Delete
    suspend fun delete(assignment: AssignmentEntity)

    @Query("DELETE FROM assignments WHERE termId = :termId")
    suspend fun deleteByTermId(termId: String)
}
