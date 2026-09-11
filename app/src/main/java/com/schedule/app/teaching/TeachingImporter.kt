package com.schedule.app.teaching

import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.AssignmentEntity
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.SettingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object TeachingImporter {

    /**
     * Import or update a single assignment item into Room database
     */
    suspend fun apply(
        item: TeachingItem,
        term: SettingEntity,
        course: CourseEntity? = null,
        chosenDate: Long? = null,
        database: AppDatabase
    ) = withContext(Dispatchers.IO) {
        val dao = database.assignmentDao()
        val existing = dao.findBySourceId(item.id)
        val defaultDue = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val targetDueDate = chosenDate ?: item.dueDate ?: defaultDue

        if (existing != null) {
            val updated = existing.copy(
                content = item.title,
                detail = item.body,
                dueDate = targetDueDate,
                importedTitle = item.title,
                importedDueDate = targetDueDate,
                importedDetail = item.body,
                sourceURL = item.sourceURL,
                courseId = course?.id ?: existing.courseId
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
                submitMethod = "北大教学网",
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

    /**
     * Check if a specific teaching network item has already been imported
     */
    suspend fun isImported(itemId: String, database: AppDatabase): Boolean = withContext(Dispatchers.IO) {
        database.assignmentDao().findBySourceId(itemId) != null
    }

    /**
     * One-click batch import all assignments from PKU teaching network.
     * Automatically fuzzy matches each item's course name with local courses in the current term.
     * Returns the count of imported/updated assignments.
     */
    suspend fun importAll(
        items: List<TeachingItem>,
        term: SettingEntity,
        courses: List<CourseEntity>,
        database: AppDatabase
    ): Int = withContext(Dispatchers.IO) {
        val assignmentItems = items.filter { it.kind == TeachingKind.ASSIGNMENT }
        var importedCount = 0

        for (item in assignmentItems) {
            val matchedCourse = findMatchingCourse(item, courses)
            apply(
                item = item,
                term = term,
                course = matchedCourse,
                chosenDate = item.dueDate,
                database = database
            )
            importedCount++
        }

        importedCount
    }

    /**
     * Fuzzy matches a TeachingItem's course title with local courses.
     * Handles teaching network formats such as:
     * - "(2024-2025-2)-计算概论A-01班" -> "计算概论A"
     * - "[2025春] 高等数学 (B)" -> "高等数学"
     */
    fun findMatchingCourse(item: TeachingItem, courses: List<CourseEntity>): CourseEntity? {
        val cleanNetworkTitle = cleanCourseTitle(item.courseTitle)
        val cleanDisplayTitle = cleanCourseTitle(item.displayCourseTitle)

        // 1. Exact match with cleaned titles
        courses.find { cleanCourseTitle(it.name) == cleanDisplayTitle || cleanCourseTitle(it.name) == cleanNetworkTitle }?.let {
            return it
        }

        // 2. Contains match (local name is part of network title, or vice versa)
        courses.find {
            val cleanLocal = cleanCourseTitle(it.name)
            cleanLocal.isNotEmpty() && (cleanDisplayTitle.contains(cleanLocal) || cleanNetworkTitle.contains(cleanLocal) || cleanLocal.contains(cleanDisplayTitle))
        }?.let {
            return it
        }

        // 3. Fallback: match first 4 characters if long enough
        courses.find {
            val cleanLocal = cleanCourseTitle(it.name)
            if (cleanLocal.length >= 3 && cleanDisplayTitle.length >= 3) {
                cleanDisplayTitle.startsWith(cleanLocal.take(3)) || cleanLocal.startsWith(cleanDisplayTitle.take(3))
            } else false
        }?.let {
            return it
        }

        return null
    }

    private fun cleanCourseTitle(raw: String): String {
        return raw
            // Remove year/term bracket prefixes like (2024-2025-1) or [2024秋]
            .replace(Regex("^[\\(\\[（【][^\\)\\]）】]+[\\)\\]）】]-?"), "")
            // Remove class / section suffixes like -01班, (01), -02
            .replace(Regex("[-_]?\\d+班?$"), "")
            .replace(Regex("[\\(\\[（【].*?[\\)\\]）】]"), "")
            .replace(Regex("\\s+"), "")
            .trim()
    }
}
