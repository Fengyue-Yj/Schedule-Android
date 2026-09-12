package com.schedule.app.teaching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TeachingParserTest {

    @Test
    fun testChineseFullDateWithTime() {
        val date = TeachingParser.parseDate("2024年10月15日 星期二 18:00")
        assertNotNull("Should parse Chinese full date with weekday and exact time", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testChineseWithZhouWeekday() {
        val date = TeachingParser.parseDate("2024年10月15日 周二 18:00")
        assertNotNull("Should parse Chinese date with 周二 and exact time", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testChineseWithParenthesizedWeekday() {
        val date = TeachingParser.parseDate("2024年10月15日(周二) 18:00")
        assertNotNull("Should parse Chinese date with (周二) and exact time", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testChineseMonthDayWithTime() {
        val date = TeachingParser.parseDate("10月20日 15:30")
        assertNotNull("Should parse Chinese month-day date with exact time", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testChineseHourOnlyWithoutMinutes() {
        val date = TeachingParser.parseDate("10月20日 18点")
        assertNotNull("Should parse 18点", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testChineseAmpmNightTime() {
        val date = TeachingParser.parseDate("10月20日 晚上8点半")
        assertNotNull("Should parse 晚上8点半", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testChineseAfternoonTime() {
        val date = TeachingParser.parseDate("2024年10月15日 下午5:30")
        assertNotNull("Should parse 下午5:30", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(17, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
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
    fun testStandardFullDateWithTime() {
        val date = TeachingParser.parseDate("2024-10-20 18:00")
        assertNotNull("Should parse standard full date with time", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testStandardFullDateWithWeekday() {
        val date = TeachingParser.parseDate("2024-10-20 周日 18:00")
        assertNotNull("Should parse standard full date with weekday", date)
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date!! }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(10, cal.get(Calendar.MONTH) + 1)
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testNoTimeDefaultsTo2359() {
        val date1 = TeachingParser.parseDate("2024年10月15日")
        assertNotNull("Should parse Chinese date without time", date1)
        val cal1 = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date1!! }
        assertEquals(2024, cal1.get(Calendar.YEAR))
        assertEquals(10, cal1.get(Calendar.MONTH) + 1)
        assertEquals(15, cal1.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal1.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal1.get(Calendar.MINUTE))

        val date2 = TeachingParser.parseDate("2024-10-20")
        assertNotNull("Should parse ISO date without time", date2)
        val cal2 = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date2!! }
        assertEquals(2024, cal2.get(Calendar.YEAR))
        assertEquals(10, cal2.get(Calendar.MONTH) + 1)
        assertEquals(20, cal2.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal2.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal2.get(Calendar.MINUTE))

        val date3 = TeachingParser.parseDate("10月20日")
        assertNotNull("Should parse month-day without time", date3)
        val cal3 = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { time = date3!! }
        assertEquals(10, cal3.get(Calendar.MONTH) + 1)
        assertEquals(20, cal3.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, cal3.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal3.get(Calendar.MINUTE))
    }

    @Test
    fun testExtractDeadlineSnippets() {
        assertEquals("10月20日 23:59", TeachingParser.extractDeadlineSnippet("截止时间：10月20日 23:59"))
        assertEquals("10/20 18:00", TeachingParser.extractDeadlineSnippet("DDL: 10/20 18:00"))
        assertEquals("10月20日 18:00", TeachingParser.extractDeadlineSnippet("请于10月20日 18:00前提交"))
        assertEquals("10月20日 23:59", TeachingParser.extractDeadlineSnippet("作业一（10月20日 23:59截止）"))
        assertEquals("2024-10-20 18:00", TeachingParser.extractDeadlineSnippet("到期日: 2024-10-20 18:00"))
        assertEquals("2024年10月15日 周二 18:00", TeachingParser.extractDeadlineSnippet("截止时间: 2024年10月15日 周二 18:00"))
    }
}
