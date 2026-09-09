package com.schedule.app.util

import com.schedule.app.data.dao.CourseDao
import com.schedule.app.data.dao.CourseMeetingDao
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.CourseMeetingEntity
import java.util.UUID

data class ImportSummary(
    var courses: Int = 0,
    var sessions: Int = 0,
    var duplicates: Int = 0,
    var rejectedRows: List<Int> = emptyList()
) {
    val message: String
        get() {
            var text = "Added $courses courses and $sessions sessions. Skipped $duplicates duplicate sessions."
            if (rejectedRows.isNotEmpty()) {
                val rows = rejectedRows.take(12).joinToString(", ")
                val dots = if (rejectedRows.size > 12) "…" else ""
                text += "\nInvalid rows skipped: $rows$dots. Use a course name, weekday 1–7 and periods 1–12 with start ≤ end."
            }
            return text
        }
}

class ImportError(message: String) : Exception(message) {
    companion object {
        fun encoding() = ImportError("Save the CSV as UTF-8 and try again.")
        fun noValidRows(rows: List<Int>): ImportError {
            val rowsStr = if (rows.isEmpty()) "" else " Invalid rows: ${rows.take(12).joinToString(", ")}."
            return ImportError("No valid courses were found. Check the CSV headers, names and period ranges.$rowsStr")
        }
    }
}

private data class CourseKey(val name: String, val teacher: String, val classroom: String) {
    constructor(n: String, t: String, c: String, dummy: Boolean = false) : this(
        n.trim().lowercase(),
        t.trim().lowercase(),
        c.trim().lowercase()
    )
}

object CourseImporter {
    suspend fun importCSV(
        csv: String, 
        termId: String, 
        courseDao: CourseDao, 
        meetingDao: CourseMeetingDao
    ): ImportSummary {
        val preview = CalendarManager.inspectCSV(csv)
        if (preview.courses.isEmpty()) {
            throw ImportError.noValidRows(preview.rejectedRows)
        }

        val existingCourses = courseDao.getCoursesForTerm(termId)
        val existingMeetings = meetingDao.getMeetingsForTerm(termId)
        
        val courseMap = mutableMapOf<CourseKey, CourseEntity>()
        for (course in existingCourses) {
            val key = CourseKey(course.name, course.teacher, course.classroom, true)
            if (!courseMap.containsKey(key)) {
                courseMap[key] = course
            }
        }

        val result = ImportSummary(rejectedRows = preview.rejectedRows)
        val newMeetings = mutableListOf<CourseMeetingEntity>()
        
        for (seed in preview.courses) {
            val key = CourseKey(seed.name, seed.teacher, seed.classroom, true)
            var course = courseMap[key]
            
            if (course == null) {
                val newCourse = CourseEntity(
                    id = UUID.randomUUID().toString(),
                    termId = termId,
                    name = seed.name,
                    teacher = seed.teacher,
                    classroom = seed.classroom,
                    weekday = seed.weekday,
                    startPeriod = seed.startPeriod,
                    endPeriod = seed.endPeriod,
                    weekPattern = seed.weekPattern.name
                )
                courseDao.insert(newCourse)
                course = newCourse
                courseMap[key] = course
                result.courses += 1
            }

            // Check if meeting exists
            val meetingExists = existingMeetings.any { 
                it.courseId == course.id && 
                it.weekday == seed.weekday && 
                it.startPeriod == seed.startPeriod && 
                it.endPeriod == seed.endPeriod && 
                it.weekPattern == seed.weekPattern.name 
            } || newMeetings.any {
                it.courseId == course.id && 
                it.weekday == seed.weekday && 
                it.startPeriod == seed.startPeriod && 
                it.endPeriod == seed.endPeriod && 
                it.weekPattern == seed.weekPattern.name 
            }

            if (meetingExists) {
                result.duplicates += 1
                continue
            }

            val meeting = CourseMeetingEntity(
                id = UUID.randomUUID().toString(),
                courseId = course.id,
                weekday = seed.weekday,
                startPeriod = seed.startPeriod,
                endPeriod = seed.endPeriod,
                weekPattern = seed.weekPattern.name
            )
            newMeetings.add(meeting)
            result.sessions += 1
        }
        
        if (newMeetings.isNotEmpty()) {
            meetingDao.insertAll(newMeetings)
        }

        return result
    }
}
