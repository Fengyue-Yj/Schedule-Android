package com.schedule.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseMeetingEntity
import com.schedule.app.data.models.CourseWithMeetings
import com.schedule.app.data.models.WeekPattern
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(
    courseWithMeetings: CourseWithMeetings,
    database: AppDatabase,
    onDismiss: () -> Unit,
    onOpenTeachingHub: ((courseId: String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteAlert by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(courseWithMeetings.course.name) }
    var teacher by remember { mutableStateOf(courseWithMeetings.course.teacher) }
    var classroom by remember { mutableStateOf(courseWithMeetings.course.classroom) }
    var courseColorHex by remember { mutableStateOf(courseWithMeetings.course.colorHex) }
    var meetingDrafts by remember { 
        mutableStateOf(
            courseWithMeetings.meetings.map {
                MeetingDraft(
                    id = it.id,
                    weekday = it.weekday,
                    startPeriod = it.startPeriod,
                    endPeriod = it.endPeriod,
                    weekPattern = it.pattern
                )
            }.ifEmpty { listOf(MeetingDraft()) }
        )
    }

    val presetColors = listOf("", "#DBF5E6", "#CCEBD8", "#E6FAF0", "#C7E6CC", "#EBF5E0", "#D6F0EB")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (isEditing) "Edit Course" else "Course Detail", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (isEditing) {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val updatedCourse = courseWithMeetings.course.copy(
                                name = name.trim(),
                                teacher = teacher.trim(),
                                classroom = classroom.trim(),
                                colorHex = courseColorHex
                            )
                            database.courseDao().update(updatedCourse)
                            
                            val draftIds = meetingDrafts.map { it.id }.toSet()
                            
                            // Delete removed
                            courseWithMeetings.meetings.filter { it.id !in draftIds }.forEach {
                                database.courseMeetingDao().delete(it)
                            }
                            
                            // Update or insert
                            meetingDrafts.forEach { draft ->
                                val meeting = CourseMeetingEntity(
                                    id = draft.id,
                                    courseId = updatedCourse.id,
                                    weekday = draft.weekday,
                                    startPeriod = draft.startPeriod,
                                    endPeriod = draft.endPeriod,
                                    weekPattern = draft.weekPattern.name
                                )
                                database.courseMeetingDao().insert(meeting)
                            }
                            isEditing = false
                            onDismiss()
                        }
                    }, enabled = name.trim().isNotEmpty() && meetingDrafts.isNotEmpty() && meetingDrafts.all { it.isValid }) {
                        Text("Save")
                    }
                } else {
                    TextButton(onClick = { isEditing = true }) {
                        Text("Edit")
                    }
                }
            }

            Text("Course Info", style = MaterialTheme.typography.titleMedium)
            if (isEditing) {
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
            } else {
                Text(courseWithMeetings.course.name, style = MaterialTheme.typography.headlineSmall)
                if (courseWithMeetings.course.teacher.isNotBlank()) {
                    Text("Teacher: ${courseWithMeetings.course.teacher}", style = MaterialTheme.typography.bodyMedium)
                }
                if (courseWithMeetings.course.classroom.isNotBlank()) {
                    Text("Classroom: ${courseWithMeetings.course.classroom}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("Schedule", style = MaterialTheme.typography.titleMedium)
            if (isEditing) {
                meetingDrafts.forEachIndexed { index, draft ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Session ${index + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (meetingDrafts.size > 1) {
                                    IconButton(onClick = {
                                        meetingDrafts = meetingDrafts.filter { it.id != draft.id }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Session", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // Weekday Selector
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("星期", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                WeekdaySelector(
                                    selectedDay = draft.weekday,
                                    onDaySelected = { day ->
                                        meetingDrafts = meetingDrafts.map {
                                            if (it.id == draft.id) it.copy(weekday = day) else it
                                        }
                                    }
                                )
                            }

                            // Periods: Start - End (customizable 1-12)
                            PeriodRangePicker(
                                startPeriod = draft.startPeriod,
                                endPeriod = draft.endPeriod,
                                onRangeChanged = { newStart, newEnd ->
                                    meetingDrafts = meetingDrafts.map {
                                        if (it.id == draft.id) it.copy(startPeriod = newStart, endPeriod = newEnd) else it
                                    }
                                }
                            )

                            // Pattern
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WeekPattern.entries.forEach { pattern ->
                                    FilterChip(
                                        selected = draft.weekPattern == pattern,
                                        onClick = {
                                            meetingDrafts = meetingDrafts.map {
                                                if (it.id == draft.id) it.copy(weekPattern = pattern) else it
                                            }
                                        },
                                        label = { Text(patternLabel(pattern)) }
                                    )
                                }
                            }
                        }
                    }
                }

                Button(onClick = { meetingDrafts = meetingDrafts + MeetingDraft() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Session")
                }
            } else {
                meetingDrafts.forEach { draft ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text("${weekdayLabel(draft.weekday)} · ${draft.startPeriod}-${draft.endPeriod}", fontWeight = FontWeight.SemiBold)
                        Text(patternLabel(draft.weekPattern), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFFDBF5E6) }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, RoundedCornerShape(18.dp))
                                .border(
                                    width = if (courseColorHex == hex) 3.dp else 1.dp,
                                    color = if (courseColorHex == hex) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { courseColorHex = hex }
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Course Color", modifier = Modifier.weight(1f))
                    val color = courseWithMeetings.course.displayColor()
                    Box(modifier = Modifier.size(28.dp, 20.dp).background(color, RoundedCornerShape(6.dp)))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTeachingHub?.invoke(courseWithMeetings.course.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "教学网作业与课件",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "查看本课程在教学网的作业与资料课件并一键下载",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showDeleteAlert = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Course")
            }

            if (showDeleteAlert) {
                AlertDialog(
                    onDismissRequest = { showDeleteAlert = false },
                    title = { Text("Delete Course?") },
                    text = { Text("This will remove the course and all related assignments and exams.") },
                    confirmButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                database.courseDao().delete(courseWithMeetings.course)
                                showDeleteAlert = false
                                onDismiss()
                            }
                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteAlert = false }) { Text("Cancel") }
                    }
                )
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun patternLabel(pattern: WeekPattern): String {
    return when (pattern) {
        WeekPattern.ALL -> "全周"
        WeekPattern.ODD -> "单周"
        WeekPattern.EVEN -> "双周"
    }
}
