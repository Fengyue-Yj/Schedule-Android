package com.schedule.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.schedule.app.data.models.ExamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE termId = :termId ORDER BY date ASC")
    fun getByTermId(termId: String?): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE termId = :termId ORDER BY date ASC")
    fun getExamsByTerm(termId: String?): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE termId = :termId ORDER BY date ASC")
    fun getExamsForTerm(termId: String?): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exam: ExamEntity)

    @Update
    suspend fun update(exam: ExamEntity)

    @Delete
    suspend fun delete(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE termId = :termId")
    suspend fun deleteByTermId(termId: String)
}
