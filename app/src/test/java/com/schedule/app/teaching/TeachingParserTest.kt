package com.schedule.app.teaching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TeachingParserTest {

    @Test
    fun testChineseFullDateWithTime() {
        val date = TeachingParser.parseDate("2024年10月15日 星期二 23:59")
        assertNotNull("Should parse Chinese full date", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testChineseMonthDayWithTime() {
        val date = TeachingParser.parseDate("10月20日 23:59")
        assertNotNull("Should parse Chinese month-day date", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testChinese24HourHandling() {
        val date = TeachingParser.parseDate("10月20日 24:00")
        assertNotNull("Should parse 24:00 time", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testStandardFullDate() {
        val date = TeachingParser.parseDate("2024-10-20 23:59")
        assertNotNull("Should parse standard full date", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testStandardMonthDayDate() {
        val date = TeachingParser.parseDate("10/20 23:59")
        assertNotNull("Should parse standard month/day date", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testExtractDeadlineSnippets() {
        assertEquals("10月20日 23:59", TeachingParser.extractDeadlineSnippet("截止时间：10月20日 23:59"))
        assertEquals("10/20 23:59", TeachingParser.extractDeadlineSnippet("DDL: 10/20 23:59"))
        assertEquals("10月20日 23:59", TeachingParser.extractDeadlineSnippet("请于10月20日 23:59前提交"))
        assertEquals("10月20日 23:59", TeachingParser.extractDeadlineSnippet("作业一（10月20日 23:59截止）"))
        assertEquals("2024-10-20 23:59", TeachingParser.extractDeadlineSnippet("到期日: 2024-10-20 23:59"))
    }
}
