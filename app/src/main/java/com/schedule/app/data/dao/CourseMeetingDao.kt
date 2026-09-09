package com.schedule.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.schedule.app.data.models.CourseMeetingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseMeetingDao {
    @Query("SELECT * FROM course_meetings WHERE courseId = :courseId")
    fun getByCourseId(courseId: String): Flow<List<CourseMeetingEntity>>

    @Query("SELECT * FROM course_meetings WHERE courseId = :courseId")
    suspend fun getMeetingsByCourseIdSync(courseId: String): List<CourseMeetingEntity>

    @Query("SELECT cm.* FROM course_meetings cm INNER JOIN courses c ON cm.courseId = c.id WHERE c.termId = :termId")
    suspend fun getMeetingsForTerm(termId: String): List<CourseMeetingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: CourseMeetingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meetings: List<CourseMeetingEntity>)

    @Delete
    suspend fun delete(meeting: CourseMeetingEntity)

    @Query("DELETE FROM course_meetings WHERE courseId = :courseId")
    suspend fun deleteByCourseId(courseId: String)
}
