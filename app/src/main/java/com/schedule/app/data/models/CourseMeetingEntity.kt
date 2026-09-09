package com.schedule.app.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "course_meetings",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class CourseMeetingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val courseId: String,
    val weekday: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekPattern: String = WeekPattern.ALL.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    val pattern: WeekPattern
        get() = try { WeekPattern.valueOf(weekPattern) } catch (e: Exception) { WeekPattern.ALL }

    fun isActive(inWeek: Int): Boolean {
        return when (pattern) {
            WeekPattern.ALL -> true
            WeekPattern.ODD -> inWeek % 2 == 1
            WeekPattern.EVEN -> inWeek % 2 == 0
        }
    }
}
