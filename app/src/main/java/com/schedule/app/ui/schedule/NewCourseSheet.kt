package com.schedule.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    var courseColorHex by remember { mutableStateOf("#FF5722") }
    var meetingDrafts by remember { mutableStateOf(listOf(MeetingDraft())) }

    val presetColors = listOf("#FF5722", "#4CAF50", "#2196F3", "#9C27B0", "#FFEB3B", "#795548")

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Weekday", modifier = Modifier.weight(1f))
                            Button(onClick = {
                                val next = if (draft.weekday == 7) 1 else draft.weekday + 1
                                meetingDrafts = meetingDrafts.map { if (it.id == draft.id) it.copy(weekday = next) else it }
                            }) {
                                Text(weekdayLabel(draft.weekday))
                            }
                        }
                        // Periods
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Periods", modifier = Modifier.weight(1f))
                            Button(onClick = {
                                val nextStart = if (draft.startPeriod == 1) 3 else if (draft.startPeriod == 3) 5 else if (draft.startPeriod == 5) 7 else if (draft.startPeriod == 7 && draft.endPeriod == 8) 7 else if (draft.startPeriod == 7) 10 else if (draft.startPeriod == 10 && draft.endPeriod == 11) 10 else 1
                                val nextEnd = if (nextStart == 1) 2 else if (nextStart == 3) 4 else if (nextStart == 5) 6 else if (nextStart == 7 && draft.endPeriod != 8) 8 else if (nextStart == 7) 9 else if (nextStart == 10 && draft.endPeriod != 11) 11 else if (nextStart == 10) 12 else 2
                                meetingDrafts = meetingDrafts.map { if (it.id == draft.id) it.copy(startPeriod = nextStart, endPeriod = nextEnd) else it }
                            }) {
                                Text("${draft.startPeriod}-${draft.endPeriod}")
                            }
                        }
                        // Week Pattern
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Week Pattern", modifier = Modifier.weight(1f))
                            Button(onClick = {
                                val next = when(draft.weekPattern) {
                                    WeekPattern.ALL -> WeekPattern.ODD
                                    WeekPattern.ODD -> WeekPattern.EVEN
                                    WeekPattern.EVEN -> WeekPattern.ALL
                                }
                                meetingDrafts = meetingDrafts.map { if (it.id == draft.id) it.copy(weekPattern = next) else it }
                            }) {
                                Text(draft.weekPattern.name)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presetColors.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(android.graphics.Color.parseColor(hex)), RoundedCornerShape(18.dp))
                            .border(
                                width = if (courseColorHex == hex) 3.dp else 0.dp,
                                color = if (courseColorHex == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
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
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        else -> "Sunday"
    }
}
