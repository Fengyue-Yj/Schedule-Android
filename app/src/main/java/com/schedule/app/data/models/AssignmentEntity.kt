package com.schedule.app.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "assignments",
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
data class AssignmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val termId: String? = null,
    val detail: String = "",
    val sourceID: String? = null,
    val sourceAccountID: String? = null,
    val sourceURL: String? = null,
    val importedTitle: String? = null,
    val importedDueDate: Long? = null,
    val importedDetail: String? = null,
    val content: String,
    val dueDate: Long,
    val submitMethod: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val courseId: String? = null
)
