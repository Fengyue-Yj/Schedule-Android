package com.schedule.app.teaching

import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.AssignmentEntity
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.SettingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object TeachingImporter {
    suspend fun apply(
        item: TeachingItem,
        term: SettingEntity,
        course: CourseEntity? = null,
        chosenDate: Long? = null,
        database: AppDatabase
    ) = withContext(Dispatchers.IO) {
        val dao = database.assignmentDao()
        val existing = dao.findBySourceId(item.id)
        val targetDueDate = chosenDate ?: item.dueDate ?: System.currentTimeMillis()

        if (existing != null) {
            val updated = existing.copy(
                content = item.title,
                detail = item.body,
                dueDate = targetDueDate,
                importedTitle = item.title,
                importedDueDate = targetDueDate,
                importedDetail = item.body,
                sourceURL = item.sourceURL
            )
            dao.update(updated)
        } else {
            val assignment = AssignmentEntity(
                id = UUID.randomUUID().toString(),
                termId = term.id,
                courseId = course?.id,
                content = item.title,
                detail = item.body,
                dueDate = targetDueDate,
                submitMethod = "PKU Teaching Network",
                isCompleted = false,
                sourceID = item.id,
                sourceURL = item.sourceURL,
                importedTitle = item.title,
                importedDueDate = targetDueDate,
                importedDetail = item.body
            )
            dao.insert(assignment)
        }
    }
}
