package com.schedule.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.ExamEntity
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamEditor(
    termId: String,
    courses: List<CourseEntity>,
    database: AppDatabase,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    
    // Default exam date: 14 days from now, 09:00
    val defaultCal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 14)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    var examDate by remember { mutableStateOf(defaultCal.timeInMillis) }
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var expandedCourseDropdown by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("New Exam") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                database.examDao().insert(
                                    ExamEntity(
                                        id = UUID.randomUUID().toString(),
                                        termId = termId,
                                        subject = subject.trim(),
                                        detail = detail.trim(),
                                        date = examDate,
                                        courseId = selectedCourseId
                                    )
                                )
                                onSave()
                                onDismiss()
                            }
                        },
                        enabled = subject.trim().isNotEmpty()
                    ) { Text("Save") }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.Spacing.page),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    placeholder = { Text("e.g. Midterm Examination") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Classroom / Detail") },
                    placeholder = { Text("e.g. Room 301, bring calculator") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Course selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedCourseDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedName = courses.find { it.id == selectedCourseId }?.name ?: "No Course Selected"
                        Text("Course: $selectedName")
                    }
                    DropdownMenu(
                        expanded = expandedCourseDropdown,
                        onDismissRequest = { expandedCourseDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedCourseId = null
                                expandedCourseDropdown = false
                            }
                        )
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.name) },
                                onClick = {
                                    selectedCourseId = course.id
                                    expandedCourseDropdown = false
                                }
                            )
                        }
                    }
                }

                // Date quick options
                Text("Date: ${dateFormat.format(examDate)}", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 7)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }
                        examDate = cal.timeInMillis
                    }) {
                        Text("+1 Week")
                    }
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 14)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }
                        examDate = cal.timeInMillis
                    }) {
                        Text("+2 Weeks")
                    }
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 30)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }
                        examDate = cal.timeInMillis
                    }) {
                        Text("+1 Month")
                    }
                }
            }
        }
    }
}
