package com.schedule.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.CourseWithMeetings
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE termId = :termId ORDER BY createdAt ASC")
    fun getByTermId(termId: String?): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE termId = :termId ORDER BY createdAt ASC")
    suspend fun getCoursesForTerm(termId: String): List<CourseEntity>

    @Transaction
    @Query("SELECT * FROM courses WHERE termId = :termId")
    fun getCourseWithMeetings(termId: String?): Flow<List<CourseWithMeetings>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: CourseEntity)

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses WHERE termId = :termId")
    suspend fun deleteByTermId(termId: String)
}
