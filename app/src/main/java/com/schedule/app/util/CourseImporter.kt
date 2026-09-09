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
            var text = "成功导入 $courses 门课程，共 $sessions 个课时节次。"
            if (duplicates > 0) {
                text += " 已自动跳过 $duplicates 个重复课时。"
            }
            if (rejectedRows.isNotEmpty()) {
                val rows = rejectedRows.take(12).joinToString(", ")
                val dots = if (rejectedRows.size > 12) "…" else ""
                text += "\n部分未能识别已跳过: 第 $rows$dots 行。"
            }
            return text
        }
}

class ImportError(message: String) : Exception(message) {
    companion object {
        fun encoding() = ImportError("请将 CSV 文件保存为 UTF-8 编码后再试。")
        fun noValidRows(rows: List<Int>): ImportError {
            val rowsStr = if (rows.isEmpty()) "" else " 未识别行号: ${rows.take(12).joinToString(", ")}。"
            return ImportError("未能从该文件中识别出有效课程。请检查 CSV 文件是否包含课程名称、星期与节次。$rowsStr")
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
