package com.schedule.app.ui.schedule

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.util.CalendarManager
import com.schedule.app.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeekDateHeaderRow(week: Int, setting: SettingEntity) {
    val isDark = isSystemInDarkTheme()
    val todayColor = if (isDark) AppTheme.colors.highlight else AppTheme.colors.primary

    val dates = getWeekDates(week, setting)
    val weekdays = getWeekdaySymbols(setting.showWeekends)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "P",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )

        dates.forEachIndexed { index, date ->
            val isToday = isToday(date)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = weekdays[index],
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isToday) todayColor else if (isDark) Color.White else Color.Black
                )
                Text(
                    text = SimpleDateFormat("M/d", Locale.getDefault()).format(date),
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) todayColor else Color.Gray
                )
            }
        }
    }
}

private fun getWeekDates(week: Int, setting: SettingEntity): List<Date> {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = CalendarManager.mondayOnOrBefore(setting.termStartDate)
    calendar.add(Calendar.DAY_OF_YEAR, (week - 1) * 7)
    val start = calendar.time

    val count = if (setting.showWeekends) 7 else 5
    val dates = mutableListOf<Date>()
    for (i in 0 until count) {
        val cal = Calendar.getInstance()
        cal.time = start
        cal.add(Calendar.DAY_OF_YEAR, i)
        dates.add(cal.time)
    }
    return dates
}

private fun getWeekdaySymbols(showWeekends: Boolean): List<String> {
    val symbols = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    return if (showWeekends) symbols else symbols.take(5)
}

private fun isToday(date: Date): Boolean {
    val today = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = date }
    return today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
           today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
}
