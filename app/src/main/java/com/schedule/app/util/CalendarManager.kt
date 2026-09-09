package com.schedule.app.util

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
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

    /**
     * Decode byte array from uploaded CSV file with automatic charset detection:
     * Handles UTF-8 with BOM, pure UTF-8, GB18030 / GBK / GB2312 (Excel default in China), and UTF-16.
     */
    fun decodeCsvBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        // Check UTF-8 BOM (EF BB BF)
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }

        // Check UTF-16 LE BOM (FF FE)
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }

        // Check UTF-16 BE BOM (FE FF)
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }

        // Try strict UTF-8 decoding
        try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val charBuffer = decoder.decode(ByteBuffer.wrap(bytes))
            val text = charBuffer.toString()
            if (!text.contains("\uFFFD")) {
                return text
            }
        } catch (_: Exception) {}

        // Fallback to GB18030 (covers GBK and GB2312, standard on Chinese Windows/Excel)
        try {
            val gbkCharset = java.nio.charset.Charset.forName("GB18030")
            val text = String(bytes, gbkCharset)
            if (!text.contains("\uFFFD")) {
                return text
            }
        } catch (_: Exception) {}

        return String(bytes, Charsets.UTF_8)
    }

    // Comprehensive Header Synonym Lists
    private val nameKeys = listOf(
        "name", "coursename", "course_name", "course", "title", "subject",
        "课程名称", "课程名", "课程", "课名", "课程全称", "科目", "名称", "活动名称", "主题", "活动"
    )

    private val teacherKeys = listOf(
        "teacher", "instructor", "lecturer", "prof", "professor",
        "任课教师", "授课教师", "教师姓名", "任课老师", "授课老师", "教师", "老师", "讲师", "教授", "主持", "description", "描述", "备注"
    )

    private val classroomKeys = listOf(
        "classroom", "location", "room", "place", "address", "venue",
        "上课地点", "上课教室", "授课地点", "教学楼", "教室", "地点", "场地", "位置", "校区"
    )

    private val weekdayKeys = listOf(
        "weekday", "day", "dayofweek", "星期", "周", "星期几", "周几", "上课星期", "授课星期", "礼拜"
    )

    private val combinedPeriodKeys = listOf(
        "period", "periods", "section", "sections", "time",
        "节次", "上课节次", "大节", "节", "时段", "课节", "上课时间"
    )

    private val startPeriodKeys = listOf(
        "startperiod", "start_period", "start", "from",
        "开始节次", "起始节次", "开始节", "起始节", "起始", "开始", "首节"
    )

    private val endPeriodKeys = listOf(
        "endperiod", "end_period", "end", "to",
        "结束节次", "终止节次", "结束节", "终止节", "结束", "末节"
    )

    private val weekPatternKeys = listOf(
        "weekpattern", "week_pattern", "pattern", "weeks", "week",
        "单双周", "周次", "周类型", "单双", "轮次", "上课周次", "周数"
    )

    fun inspectCSV(csv: String): ImportPreview {
        var cleaned = csv.replace("\r\n", "\n").replace("\r", "\n")
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1)
        }

        // If this is an iCalendar (.ics) format file
        if (cleaned.contains("BEGIN:VCALENDAR") || cleaned.contains("BEGIN:VEVENT")) {
            return inspectICS(cleaned)
        }

        // Auto-detect delimiter: comma, tab, or semicolon
        val firstLine = cleaned.lineSequence().firstOrNull { it.trim().isNotEmpty() } ?: ""
        val delimiter = when {
            firstLine.count { it == '\t' } >= 2 -> '\t'
            firstLine.count { it == ';' } >= 2 && firstLine.count { it == ',' } < 2 -> ';'
            else -> ','
        }

        val (rows, unterminatedQuotes) = parseCSVRows(cleaned, delimiter)
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

            // 1. Course Name (required)
            val name = valueFor(nameKeys, row, headerMap)?.trim()
            if (name.isNullOrEmpty()) {
                rejectedRows.add(index + 1)
                continue
            }

            // 2. Teacher & Classroom (optional, defaults to "")
            val teacher = valueFor(teacherKeys, row, headerMap)?.trim() ?: ""
            val classroom = valueFor(classroomKeys, row, headerMap)?.trim() ?: ""

            // 3. Weekday & Periods
            val weekdayString = valueFor(weekdayKeys, row, headerMap)
            val startString = valueFor(startPeriodKeys, row, headerMap)
            val endString = valueFor(endPeriodKeys, row, headerMap)
            val combinedPeriodString = valueFor(combinedPeriodKeys, row, headerMap)
            val weekPatternString = valueFor(weekPatternKeys, row, headerMap)

            // Parse weekday
            var weekday = weekdayString?.let { parseWeekday(it) }
            // If weekday is not found in weekday column, check combinedPeriodString (e.g. "周一 1-2节")
            if (weekday == null && combinedPeriodString != null) {
                weekday = parseWeekday(combinedPeriodString)
            }

            // Parse periods
            val periodRange = parsePeriodRange(startString, endString, combinedPeriodString)

            if (weekday == null || periodRange == null) {
                // Try fallback row inspection
                val fallback = detectRowData(row)
                if (fallback != null) {
                    courses.add(
                        CourseSeed(
                            name = name,
                            teacher = teacher.ifEmpty { fallback.teacher },
                            classroom = classroom.ifEmpty { fallback.classroom },
                            weekday = fallback.weekday,
                            startPeriod = fallback.startPeriod,
                            endPeriod = fallback.endPeriod,
                            weekPattern = fallback.weekPattern
                        )
                    )
                    continue
                }

                rejectedRows.add(index + 1)
                continue
            }

            val pattern = parseWeekPattern(weekPatternString)

            courses.add(
                CourseSeed(
                    name = name,
                    teacher = teacher,
                    classroom = classroom,
                    weekday = weekday,
                    startPeriod = periodRange.first,
                    endPeriod = periodRange.second,
                    weekPattern = pattern
                )
            )
        }

        return ImportPreview(courses, rejectedRows)
    }

    private fun parseCSVRows(csv: String, delimiter: Char): Pair<List<List<String>>, Boolean> {
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
            } else if (char == delimiter && !inQuotes) {
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
        val allSynonyms = nameKeys + teacherKeys + classroomKeys + weekdayKeys +
                combinedPeriodKeys + startPeriodKeys + endPeriodKeys + weekPatternKeys

        val map = mutableMapOf<String, Int>()
        for ((index, raw) in header.withIndex()) {
            val key = raw.trim().lowercase()
            if (key.isNotEmpty()) {
                map[key] = index
            }
        }
        val hasHeader = header.any { h ->
            val clean = h.trim().lowercase()
            allSynonyms.any { syn -> clean == syn || clean.contains(syn) }
        }
        return if (hasHeader) map else emptyMap()
    }

    private fun valueFor(keys: List<String>, row: List<String>, headerMap: Map<String, Int>): String? {
        if (headerMap.isEmpty()) {
            val index = defaultIndex(keys)
            return if (index != null && index < row.size) row[index] else null
        }

        // Exact match first
        for (key in keys) {
            val normalized = key.lowercase()
            val index = headerMap[normalized]
            if (index != null && index < row.size) {
                return row[index]
            }
        }
        // Substring match
        for (key in keys) {
            val entry = headerMap.entries.firstOrNull { it.key.contains(key.lowercase()) }
            if (entry != null && entry.value < row.size) {
                return row[entry.value]
            }
        }

        return null
    }

    private fun defaultIndex(keys: List<String>): Int? {
        val mapping = mapOf(
            "name" to 0, "课程" to 0, "课程名" to 0, "课程名称" to 0,
            "teacher" to 1, "老师" to 1, "教师" to 1, "任课教师" to 1,
            "classroom" to 2, "教室" to 2, "地点" to 2, "上课地点" to 2,
            "weekday" to 3, "星期" to 3, "周" to 3, "星期几" to 3,
            "startperiod" to 4, "开始节次" to 4, "起始节次" to 4, "节次" to 4,
            "endperiod" to 5, "结束节次" to 5, "终止节次" to 5,
            "weekpattern" to 6, "单双周" to 6, "周次" to 6
        )

        for (key in keys) {
            val index = mapping[key.lowercase()]
            if (index != null) return index
        }
        return null
    }

    fun parseWeekday(raw: String): Int? {
        val clean = raw.trim().lowercase()
        val num = clean.toIntOrNull()
        if (num != null && num in 1..7) return num

        if (clean.contains("一") || clean.contains("mon") || clean == "1") return 1
        if (clean.contains("二") || clean.contains("tue") || clean == "2") return 2
        if (clean.contains("三") || clean.contains("wed") || clean == "3") return 3
        if (clean.contains("四") || clean.contains("thu") || clean == "4") return 4
        if (clean.contains("五") || clean.contains("fri") || clean == "5") return 5
        if (clean.contains("六") || clean.contains("sat") || clean == "6") return 6
        if (clean.contains("日") || clean.contains("天") || clean.contains("sun") || clean == "7") return 7
        return null
    }

    fun parsePeriodRange(startString: String?, endString: String?, combinedString: String?): Pair<Int, Int>? {
        // Check combined or startString for a range like "1-2节", "3~4", "1-3", "第1-2节"
        val candidate = (combinedString ?: startString ?: "").trim()
        val numbers = Regex("""\d+""").findAll(candidate).map { it.value.toInt() }.toList()
        if (numbers.size >= 2) {
            val s = numbers[0].coerceIn(1, 12)
            val e = numbers[1].coerceIn(s, 12)
            return Pair(s, e)
        } else if (numbers.size == 1 && (endString == null || endString.isBlank())) {
            val s = numbers[0].coerceIn(1, 12)
            return Pair(s, s)
        }

        // Separate start and end
        val s = startString?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }?.coerceIn(1, 12)
        val e = endString?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() }?.coerceIn(1, 12)
        if (s != null && e != null) {
            return Pair(minOf(s, e), maxOf(s, e))
        } else if (s != null) {
            return Pair(s, s)
        }
        return null
    }

    fun parseWeekPattern(raw: String?): WeekPattern {
        if (raw.isNullOrBlank()) return WeekPattern.ALL
        val clean = raw.trim().lowercase()
        if (clean.contains("单") || clean.contains("odd")) return WeekPattern.ODD
        if (clean.contains("双") || clean.contains("even")) return WeekPattern.EVEN
        // Any other specification like "1-16周", "前八周", "每周" defaults safely to ALL
        return WeekPattern.ALL
    }

    private data class FallbackRow(
        val teacher: String,
        val classroom: String,
        val weekday: Int,
        val startPeriod: Int,
        val endPeriod: Int,
        val weekPattern: WeekPattern
    )

    private fun detectRowData(row: List<String>): FallbackRow? {
        var foundWeekday: Int? = null
        var foundPeriod: Pair<Int, Int>? = null
        var pattern = WeekPattern.ALL
        val otherCells = mutableListOf<String>()

        for (cell in row) {
            val text = cell.trim()
            if (text.isEmpty()) continue

            if (foundWeekday == null) {
                val w = parseWeekday(text)
                if (w != null) {
                    foundWeekday = w
                    continue
                }
            }

            if (foundPeriod == null) {
                val p = parsePeriodRange(null, null, text)
                if (p != null) {
                    foundPeriod = p
                    continue
                }
            }

            if (text.contains("单") || text.contains("双")) {
                pattern = parseWeekPattern(text)
                continue
            }

            otherCells.add(text)
        }

        if (foundWeekday != null && foundPeriod != null) {
            val teacher = otherCells.getOrNull(0) ?: ""
            val classroom = otherCells.getOrNull(1) ?: ""
            return FallbackRow(teacher, classroom, foundWeekday, foundPeriod.first, foundPeriod.second, pattern)
        }
        return null
    }

    fun inspectICS(ics: String): ImportPreview {
        val courses = mutableListOf<CourseSeed>()
        val events = ics.split("BEGIN:VEVENT")
        for (event in events.drop(1)) {
            val chunk = event.substringBefore("END:VEVENT")
            var summary = ""
            var location = ""
            var description = ""
            var dtstart = ""
            var dtend = ""
            var rrule = ""

            for (line in chunk.lines()) {
                val trimmed = line.trim()
                if (trimmed.startsWith("SUMMARY:", ignoreCase = true)) {
                    summary = trimmed.substringAfter(":").trim()
                } else if (trimmed.startsWith("LOCATION:", ignoreCase = true)) {
                    location = trimmed.substringAfter(":").trim()
                } else if (trimmed.startsWith("DESCRIPTION:", ignoreCase = true)) {
                    description = trimmed.substringAfter(":").trim()
                } else if (trimmed.startsWith("DTSTART", ignoreCase = true)) {
                    dtstart = trimmed.substringAfter(":").trim()
                } else if (trimmed.startsWith("DTEND", ignoreCase = true)) {
                    dtend = trimmed.substringAfter(":").trim()
                } else if (trimmed.startsWith("RRULE:", ignoreCase = true)) {
                    rrule = trimmed.substringAfter(":").trim()
                }
            }

            if (summary.isBlank() || dtstart.isBlank()) continue

            val timeMatch = Regex("""(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})""").find(dtstart)
            if (timeMatch != null) {
                val (year, month, day, hourStr, minStr) = timeMatch.destructured
                val cal = Calendar.getInstance().apply {
                    set(year.toInt(), month.toInt() - 1, day.toInt(), hourStr.toInt(), minStr.toInt())
                }
                val weekday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
                val startPeriod = hourToPeriod(hourStr.toInt(), minStr.toInt())

                val endPeriod = if (dtend.isNotBlank()) {
                    val endMatch = Regex("""(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})""").find(dtend)
                    if (endMatch != null) {
                        val endHour = endMatch.groupValues[4].toInt()
                        val endMin = endMatch.groupValues[5].toInt()
                        hourToPeriod(endHour, endMin).coerceAtLeast(startPeriod)
                    } else startPeriod
                } else startPeriod

                val pattern = if (rrule.contains("INTERVAL=2")) WeekPattern.ODD else WeekPattern.ALL

                courses.add(
                    CourseSeed(
                        name = summary,
                        teacher = description,
                        classroom = location,
                        weekday = weekday,
                        startPeriod = startPeriod.coerceIn(1, 12),
                        endPeriod = endPeriod.coerceIn(startPeriod, 12),
                        weekPattern = pattern
                    )
                )
            }
        }
        return ImportPreview(courses)
    }

    private fun hourToPeriod(hour: Int, min: Int): Int {
        return when {
            hour < 8 || (hour == 8 && min <= 55) -> 1
            hour == 9 || (hour == 8 && min > 55) -> 2
            hour == 10 && min <= 55 -> 3
            hour == 11 || (hour == 10 && min > 55) -> 4
            hour == 13 && min <= 55 -> 5
            hour == 14 || (hour == 13 && min > 55) -> 6
            hour == 15 && min <= 55 -> 7
            hour == 16 || (hour == 15 && min > 55) -> 8
            hour == 17 -> 9
            hour == 18 || (hour == 19 && min <= 20) -> 10
            hour == 19 -> 11
            hour >= 20 -> 12
            else -> 1
        }
    }
}


