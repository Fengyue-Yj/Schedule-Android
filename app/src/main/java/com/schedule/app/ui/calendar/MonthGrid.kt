package com.schedule.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.models.AssignmentEntity
import com.schedule.app.data.models.ExamEntity
import com.schedule.app.ui.theme.AppTheme
import java.util.Calendar

data class MonthDay(
    val date: Long,
    val inCurrentMonth: Boolean
) {
    val isToday: Boolean
        get() = isSameDay(System.currentTimeMillis(), date)
    
    val numberString: String
        get() {
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            return cal.get(Calendar.DAY_OF_MONTH).toString()
        }
}

@Composable
fun MonthGrid(
    currentMonth: Long,
    selectedDate: Long?,
    assignments: List<AssignmentEntity>,
    exams: List<ExamEntity>,
    onSelectDate: (Long) -> Unit
) {
    val weekStartsOnMonday = true
    val days = generateMonthDays(currentMonth, weekStartsOnMonday)
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        userScrollEnabled = false
    ) {
        items(days) { day ->
            DayCell(
                day = day,
                selectedDate = selectedDate,
                assignments = assignments,
                exams = exams,
                onSelect = onSelectDate
            )
        }
    }
}

private fun generateMonthDays(currentMonth: Long, weekStartsOnMonday: Boolean): List<MonthDay> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = currentMonth
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday
    
    val leading = if (weekStartsOnMonday) {
        (firstWeekday - Calendar.MONDAY + 7) % 7
    } else {
        (firstWeekday - Calendar.SUNDAY + 7) % 7
    }
    
    val result = mutableListOf<MonthDay>()
    val totalCells = 42
    
    for (index in 0 until totalCells) {
        val offset = index - leading
        val dateCal = cal.clone() as Calendar
        dateCal.add(Calendar.DAY_OF_MONTH, offset)
        
        val inCurrentMonth = offset in 0 until daysInMonth
        result.add(MonthDay(dateCal.timeInMillis, inCurrentMonth))
    }
    
    return result
}

@Composable
private fun DayCell(
    day: MonthDay,
    selectedDate: Long?,
    assignments: List<AssignmentEntity>,
    exams: List<ExamEntity>,
    onSelect: (Long) -> Unit
) {
    val isSelected = selectedDate?.let { isSameDay(it, day.date) } ?: false
    val isToday = day.isToday
    
    val isDark = isSystemInDarkTheme()
    
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onSurface
        isToday -> AppTheme.colors.deepGreen
        day.inCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    
    val fillColor = when {
        isSelected -> if (isDark) Color.Transparent else Color.White
        isToday -> AppTheme.colors.highlight
        else -> Color.Transparent
    }
    
    val strokeColor = when {
        isSelected -> AppTheme.colors.deepGreen
        isToday -> AppTheme.colors.deepGreen.copy(alpha = 0.4f)
        else -> Color.Transparent
    }
    
    val assignmentCount = assignments.count { isSameDay(it.dueDate, day.date) }
    val examCount = exams.count { isSameDay(it.date, day.date) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelect(day.date) }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(fillColor, RoundedCornerShape(10.dp))
                .border(if (isSelected || isToday) 1.2.dp else 0.dp, strokeColor, RoundedCornerShape(10.dp))
        ) {
            Text(
                text = day.numberString,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(8.dp)
        ) {
            repeat(minOf(assignmentCount, 3)) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Color.Blue.copy(alpha = 0.8f), CircleShape)
                )
            }
            repeat(minOf(examCount, 3)) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Color(0xFFE91E63).copy(alpha = 0.85f), CircleShape) // Pink
                )
            }
        }
    }
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
