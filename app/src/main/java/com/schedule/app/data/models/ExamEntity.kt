package com.schedule.app.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SettingEntity::class,
            parentColumns = ["id"],
            childColumns = ["termId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("courseId"), Index("termId")]
)
data class ExamEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val termId: String? = null,
    val subject: String,
    val detail: String,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val courseId: String? = null
)
