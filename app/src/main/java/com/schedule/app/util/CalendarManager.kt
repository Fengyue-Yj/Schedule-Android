package com.schedule.app.util

import java.util.Calendar
import java.util.UUID

import com.schedule.app.data.models.WeekPattern

data class CourseSeed(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val teacher: String,
    val classroom: String,
    val weekday: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val weekPattern: WeekPattern
)

data class ImportPreview(
    val courses: List<CourseSeed> = emptyList(),
    val rejectedRows: List<Int> = emptyList()
)

object CalendarManager {

    fun mondayOnOrBefore(date: Long): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Swift offset: (weekday + 5) % 7 where Sun=1, Mon=2...
        val weekday = calendar.get(Calendar.DAY_OF_WEEK)
        val offset = (weekday + 5) % 7
        calendar.add(Calendar.DAY_OF_YEAR, -offset)
        return calendar.timeInMillis
    }

    fun currentWeekIndex(termStartDate: Long, totalWeeks: Int): Int {
        val start = mondayOnOrBefore(termStartDate)
        val todayCal = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        val today = todayCal.timeInMillis

        val dayDiff = ((today - start) / (1000 * 60 * 60 * 24)).toInt()
        val week = (dayDiff / 7) + 1
        return week.coerceIn(1, totalWeeks.coerceAtLeast(1))
    }

    fun parseCourses(csv: String): List<CourseSeed> {
        return inspectCSV(csv).courses
    }

    fun inspectCSV(csv: String): ImportPreview {
        var cleaned = csv.replace("\r\n", "\n").replace("\r", "\n")
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1)
        }
        val (rows, unterminatedQuotes) = parseCSVRows(cleaned)
        if (unterminatedQuotes) {
            return ImportPreview(rejectedRows = listOf(rows.size + 1))
        }

        val firstIndex = rows.indexOfFirst { row -> row.any { it.trim().isNotEmpty() } }
        if (firstIndex == -1) return ImportPreview()

        val headerMap = headerIndexMap(rows[firstIndex])
        val startIndex = if (headerMap.isEmpty()) firstIndex else firstIndex + 1

        val courses = mutableListOf<CourseSeed>()
        val rejectedRows = mutableListOf<Int>()

        for (index in startIndex until rows.size) {
            val row = rows[index]
            if (row.all { it.trim().isEmpty() }) continue

            val name = valueFor(listOf("name", "课程", "课程名"), row, headerMap)?.trim()
            val teacher = valueFor(listOf("teacher", "老师", "教师"), row, headerMap)?.trim()
            val classroom = valueFor(listOf("classroom", "教室", "地点"), row, headerMap)?.trim()
            val weekdayString = valueFor(listOf("weekday", "星期", "周", "星期几"), row, headerMap)
            val startString = valueFor(listOf("startperiod", "开始节次", "起始节次", "开始节"), row, headerMap)
            val endString = valueFor(listOf("endperiod", "结束节次", "终止节次", "结束节"), row, headerMap)
            val weekPatternString = valueFor(listOf("weekpattern", "单双周", "周次"), row, headerMap) ?: ""

            if (name.isNullOrEmpty() || teacher == null || classroom == null || 
                weekdayString == null || startString == null || endString == null) {
                rejectedRows.add(index + 1)
                continue
            }

            val weekday = parseWeekday(weekdayString)
            val start = startString.trim().toIntOrNull()
            val end = endString.trim().toIntOrNull()
            val pattern = parseWeekPattern(weekPatternString)

            if (weekday == null || start == null || end == null || 
                start !in 1..12 || end !in start..12 || pattern == null) {
                rejectedRows.add(index + 1)
                continue
            }

            courses.add(
                CourseSeed(
                    name = name,
                    teacher = teacher,
                    classroom = classroom,
                    weekday = weekday,
                    startPeriod = start,
                    endPeriod = end,
                    weekPattern = pattern
                )
            )
        }

        return ImportPreview(courses, rejectedRows)
    }

    private fun parseCSVRows(csv: String): Pair<List<List<String>>, Boolean> {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        val chars = csv.toCharArray()
        var index = 0

        while (index < chars.size) {
            val char = chars[index]
            if (char == '"') {
                if (inQuotes && index + 1 < chars.size && chars[index + 1] == '"') {
                    currentField.append('"')
                    index++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (char == ',' && !inQuotes) {
                currentRow.add(currentField.toString())
                currentField.clear()
            } else if ((char == '\n' || char == '\r') && !inQuotes) {
                if (char == '\r' && index + 1 < chars.size && chars[index + 1] == '\n') {
                    index++
                }
                currentRow.add(currentField.toString())
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentField.clear()
            } else {
                currentField.append(char)
            }
            index++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }

        return Pair(rows, inQuotes)
    }

    private fun headerIndexMap(header: List<String>): Map<String, Int> {
        val knownHeaders = setOf(
            "name", "课程", "课程名",
            "teacher", "老师", "教师",
            "classroom", "教室", "地点",
            "weekday", "星期", "周", "星期几",
            "startperiod", "开始节次", "起始节次", "开始节",
            "endperiod", "结束节次", "终止节次", "结束节",
            "weekpattern", "单双周", "周次"
        )

        val map = mutableMapOf<String, Int>()
        for ((index, raw) in header.withIndex()) {
            val key = raw.trim().lowercase()
            if (key.isNotEmpty()) {
                map[key] = index
            }
        }
        val hasHeader = header.any { knownHeaders.contains(it.trim().lowercase()) }
        return if (hasHeader) map else emptyMap()
    }

    private fun valueFor(keys: List<String>, row: List<String>, headerMap: Map<String, Int>): String? {
        if (headerMap.isEmpty()) {
            val index = defaultIndex(keys)
            return if (index != null && index < row.size) row[index] else null
        }

        for (key in keys) {
            val normalized = key.lowercase()
            val index = headerMap[normalized]
            if (index != null && index < row.size) {
                return row[index]
            }
        }

        return null
    }

    private fun defaultIndex(keys: List<String>): Int? {
        val mapping = mapOf(
            "name" to 0, "课程" to 0, "课程名" to 0,
            "teacher" to 1, "老师" to 1, "教师" to 1,
            "classroom" to 2, "教室" to 2, "地点" to 2,
            "weekday" to 3, "星期" to 3, "周" to 3,
            "startperiod" to 4, "开始节次" to 4, "起始节次" to 4,
            "endperiod" to 5, "结束节次" to 5, "终止节次" to 5,
            "weekpattern" to 6, "单双周" to 6, "周次" to 6
        )

        for (key in keys) {
            val index = mapping[key.lowercase()]
            if (index != null) return index
        }
        return null
    }

    private fun parseWeekday(raw: String): Int? {
        val value = raw.trim().lowercase()
        val numeric = value.toIntOrNull()
        if (numeric != null && numeric in 1..7) return numeric

        val map = mapOf(
            "mon" to 1, "monday" to 1, "周一" to 1, "星期一" to 1, "一" to 1,
            "tue" to 2, "tues" to 2, "tuesday" to 2, "周二" to 2, "星期二" to 2, "二" to 2,
            "wed" to 3, "weds" to 3, "wednesday" to 3, "周三" to 3, "星期三" to 3, "三" to 3,
            "thu" to 4, "thur" to 4, "thurs" to 4, "thursday" to 4, "周四" to 4, "星期四" to 4, "四" to 4,
            "fri" to 5, "friday" to 5, "周五" to 5, "星期五" to 5, "五" to 5,
            "sat" to 6, "saturday" to 6, "周六" to 6, "星期六" to 6, "六" to 6,
            "sun" to 7, "sunday" to 7, "周日" to 7, "周天" to 7, "星期日" to 7, "日" to 7
        )
        return map[value]
    }

    private fun parseWeekPattern(raw: String): WeekPattern? {
        return when (raw.trim().lowercase()) {
            "", "all", "全周", "全部", "每周" -> WeekPattern.ALL
            "odd", "单", "单周" -> WeekPattern.ODD
            "even", "双", "双周" -> WeekPattern.EVEN
            else -> null
        }
    }
}
