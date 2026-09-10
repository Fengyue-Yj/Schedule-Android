package com.schedule.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.CourseMeetingEntity
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.data.models.WeekPattern
import kotlinx.coroutines.launch
import java.util.UUID

data class MeetingDraft(
    val id: String = UUID.randomUUID().toString(),
    var weekday: Int = 1,
    var startPeriod: Int = 1,
    var endPeriod: Int = 2,
    var weekPattern: WeekPattern = WeekPattern.ALL
) {
    val isValid: Boolean get() = weekday in 1..7 && startPeriod in 1..12 && endPeriod >= startPeriod && endPeriod <= 12
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCourseSheet(
    term: SettingEntity,
    database: AppDatabase,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var courseColorHex by remember { mutableStateOf("") }
    var meetingDrafts by remember { mutableStateOf(listOf(MeetingDraft())) }

    val presetColors = listOf("", "#DBF5E6", "#CCEBD8", "#E6FAF0", "#C7E6CC", "#EBF5E0", "#D6F0EB")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("New Course", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = { Text("Teacher") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = classroom,
                onValueChange = { classroom = it },
                label = { Text("Classroom") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Schedule", style = MaterialTheme.typography.titleMedium)
            meetingDrafts.forEach { draft ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Session", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (meetingDrafts.size > 1) {
                                IconButton(onClick = {
                                    meetingDrafts = meetingDrafts.filter { it.id != draft.id }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        // Weekday Picker
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("星期", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (1..7).forEach { day ->
                                    FilterChip(
                                        selected = draft.weekday == day,
                                        onClick = {
                                            meetingDrafts = meetingDrafts.map { if (it.id == draft.id) it.copy(weekday = day) else it }
                                        },
                                        label = { Text(weekdayLabel(day)) }
                                    )
                                }
                            }
                        }

                        // Periods Picker
                        PeriodRangePicker(
                            startPeriod = draft.startPeriod,
                            endPeriod = draft.endPeriod,
                            onRangeChanged = { newStart, newEnd ->
                                meetingDrafts = meetingDrafts.map {
                                    if (it.id == draft.id) it.copy(startPeriod = newStart, endPeriod = newEnd) else it
                                }
                            }
                        )

                        // Week Pattern
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("单双周", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                WeekPattern.entries.forEach { pattern ->
                                    FilterChip(
                                        selected = draft.weekPattern == pattern,
                                        onClick = {
                                            meetingDrafts = meetingDrafts.map { if (it.id == draft.id) it.copy(weekPattern = pattern) else it }
                                        },
                                        label = {
                                            Text(
                                                when(pattern) {
                                                    WeekPattern.ALL -> "全周"
                                                    WeekPattern.ODD -> "单周"
                                                    WeekPattern.EVEN -> "双周"
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            TextButton(onClick = { meetingDrafts = meetingDrafts + MeetingDraft() }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Session")
            }

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto (Palette) option
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .background(
                            color = if (courseColorHex.isEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(
                            width = if (courseColorHex.isEmpty()) 2.dp else 0.5.dp,
                            color = if (courseColorHex.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { courseColorHex = "" }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "自动(柔和绿)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (courseColorHex.isEmpty()) FontWeight.Bold else FontWeight.Normal,
                        color = if (courseColorHex.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                presetColors.filter { it.isNotEmpty() }.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(android.graphics.Color.parseColor(hex)), RoundedCornerShape(18.dp))
                            .border(
                                width = if (courseColorHex == hex) 3.dp else 1.dp,
                                color = if (courseColorHex == hex) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { courseColorHex = hex }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val newCourse = CourseEntity(
                                id = UUID.randomUUID().toString(),
                                termId = term.id,
                                name = name.trim(),
                                teacher = teacher.trim(),
                                classroom = classroom.trim(),
                                colorHex = courseColorHex,
                                createdAt = System.currentTimeMillis()
                            )
                            database.courseDao().insert(newCourse)
                            
                            meetingDrafts.forEach { draft ->
                                val meeting = CourseMeetingEntity(
                                    id = UUID.randomUUID().toString(),
                                    courseId = newCourse.id,
                                    weekday = draft.weekday,
                                    startPeriod = draft.startPeriod,
                                    endPeriod = draft.endPeriod,
                                    weekPattern = draft.weekPattern.name
                                )
                                database.courseMeetingDao().insert(meeting)
                            }
                            onDismiss()
                        }
                    },
                    enabled = name.trim().isNotEmpty() && meetingDrafts.isNotEmpty() && meetingDrafts.all { it.isValid }
                ) {
                    Text("Save")
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

fun weekdayLabel(day: Int): String {
    return when (day) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }
}
