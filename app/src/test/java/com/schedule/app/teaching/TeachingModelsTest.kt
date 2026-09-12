package com.schedule.app.teaching

import org.junit.Assert.assertEquals
import org.junit.Test

class TeachingModelsTest {

    @Test
    fun testReadableTitleWithColonAndPkuLongCourseCode() {
        val raw = "(2024-2025-2)-04832540-0007802832-1:高等代数(I)(25-26学年第2学期)"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("高等代数(I)", cleaned)
    }

    @Test
    fun testReadableTitleWithStandardColon() {
        val raw = "04832540-0007802832-1: 高等代数"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("高等代数", cleaned)
    }

    @Test
    fun testReadableTitleWithYearCodeWithoutBracket() {
        val raw = "2024-2025-2-04832540-0007802832-1: 高等代数"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("高等代数", cleaned)
    }

    @Test
    fun testReadableTitleWithChineseSemesterPrefix() {
        val raw = "(2024-2025学年第二学期)-04830110-01: 算法设计与分析"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("算法设计与分析", cleaned)
    }

    @Test
    fun testReadableTitleWithSquareBracket() {
        val raw = "[2024-2025-2]-04832540-1: 操作系统"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("操作系统", cleaned)
    }

    @Test
    fun testReadableTitleWithChineseBracket() {
        val raw = "【2024-2025-2】04832540-1: 计算机系统导论"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("计算机系统导论", cleaned)
    }

    @Test
    fun testReadableTitleWithSimpleCourseCode() {
        val raw = "04830110: 计算概论A"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("计算概论A", cleaned)
    }

    @Test
    fun testReadableTitleWithUnderscoreDelimiter() {
        val raw = "04832540_0007802832_1_毛泽东思想和中国特色社会主义理论体系概论"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("毛泽东思想和中国特色社会主义理论体系概论", cleaned)
    }

    @Test
    fun testReadableTitleWithSpaceDelimiterNoColon() {
        val raw = "04832540-0007802832-1 高等代数(I)"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("高等代数(I)", cleaned)
    }

    @Test
    fun testReadableTitleWithHyphenNoColon() {
        val raw = "(2024-2025-2)-04832540-0007802832-1-高等代数"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("高等代数", cleaned)
    }

    @Test
    fun testReadableTitleWithClassSuffix() {
        val raw = "04830110: 软件工程导论(01班)"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("软件工程导论", cleaned)
    }

    @Test
    fun testReadableTitlePreservesParenthesesInCourseName() {
        val raw1 = "大学英语(三)"
        assertEquals("大学英语(三)", TeachingCourse.readableTitle(raw1))

        val raw2 = "(2024-2025-2)-04832540: 大学物理(乙)"
        assertEquals("大学物理(乙)", TeachingCourse.readableTitle(raw2))

        val raw3 = "[2025春] 高等数学 (B)"
        assertEquals("高等数学 (B)", TeachingCourse.readableTitle(raw3))
    }

    @Test
    fun testReadableTitleWithEnglishCourse() {
        val raw = "CS101: Introduction to Computer Science"
        val cleaned = TeachingCourse.readableTitle(raw)
        assertEquals("Introduction to Computer Science", cleaned)
    }
}
