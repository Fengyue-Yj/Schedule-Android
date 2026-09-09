package com.schedule.app.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.models.CourseWithMeetings
import com.schedule.app.data.models.SettingEntity

@Composable
fun WeekGridView(
    week: Int,
    setting: SettingEntity,
    courses: List<CourseWithMeetings>,
    onCourseTap: (CourseWithMeetings) -> Unit
) {
    val cellHeight = 56.dp
    val rowHeaderWidth = 36.dp
    val visibleWeekdays = if (setting.showWeekends) (1..7).toList() else (1..5).toList()
    val totalPeriods = maxOf(12, setting.periodTimeStrings.size)

    val isDark = isSystemInDarkTheme()
    val gridLineColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val totalWidth = maxWidth
        val gridWidth = maxOf(totalWidth - rowHeaderWidth, 1.dp)
        val cellWidth = maxOf(gridWidth / visibleWeekdays.size, 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Period Column
            Column(modifier = Modifier.width(rowHeaderWidth)) {
                for (period in 1..totalPeriods) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellHeight)
                            .border(BorderStroke(0.5.dp, gridLineColor))
                    ) {
                        Text(
                            text = period.toString(),
                            modifier = Modifier.fillMaxSize().padding(top = 4.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Grid & Courses Box
            Box(
                modifier = Modifier
                    .width(gridWidth)
                    .height(cellHeight * totalPeriods)
            ) {
                // Background Grid
                Row {
                    for (col in visibleWeekdays) {
                        Column(modifier = Modifier.width(cellWidth)) {
                            for (period in 1..totalPeriods) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(cellHeight)
                                        .border(BorderStroke(0.5.dp, gridLineColor))
                                )
                            }
                        }
                    }
                }

                // Courses
                courses.forEach { courseWithMeetings ->
                    courseWithMeetings.meetings.filter { it.isActive(week) }.forEach { meeting ->
                        val colIndex = visibleWeekdays.indexOf(meeting.weekday)
                        if (colIndex != -1 && meeting.startPeriod in 1..totalPeriods) {
                            val endPeriod = minOf(meeting.endPeriod, totalPeriods)
                            val span = maxOf(1, endPeriod - meeting.startPeriod + 1)
                            
                            val xOffset = cellWidth * colIndex + 2.dp
                            val yOffset = cellHeight * (meeting.startPeriod - 1) + 2.dp
                            val blockWidth = maxOf(cellWidth - 4.dp, 12.dp)
                            val blockHeight = maxOf(cellHeight * span - 4.dp, 12.dp)

                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = xOffset, y = yOffset)
                                    .size(width = blockWidth, height = blockHeight)
                                    .clickable { onCourseTap(courseWithMeetings) }
                            ) {
                                CourseBlock(course = courseWithMeetings.course, isDark = isDark)
                            }
                        }
                    }
                }
            }
        }
    }
}
