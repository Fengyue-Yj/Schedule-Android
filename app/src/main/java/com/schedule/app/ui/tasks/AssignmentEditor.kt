package com.schedule.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.AssignmentEntity
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentEditor(
    termId: String,
    courses: List<CourseEntity>,
    database: AppDatabase,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var submitMethod by remember { mutableStateOf("") }
    
    // Default due date: today 23:59
    val defaultCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 0)
    }
    var dueDate by remember { mutableStateOf(defaultCal.timeInMillis) }
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var expandedCourseDropdown by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("New Assignment") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                database.assignmentDao().insert(
                                    AssignmentEntity(
                                        id = UUID.randomUUID().toString(),
                                        termId = termId,
                                        content = content.trim(),
                                        dueDate = dueDate,
                                        submitMethod = submitMethod.trim(),
                                        courseId = selectedCourseId,
                                        isCompleted = false,
                                        detail = "",
                                        sourceURL = null
                                    )
                                )
                                onSave()
                                onDismiss()
                            }
                        },
                        enabled = content.trim().isNotEmpty()
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
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    placeholder = { Text("e.g. Read Chapter 4") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = submitMethod,
                    onValueChange = { submitMethod = it },
                    label = { Text("Submission Method") },
                    placeholder = { Text("e.g. Canvas / In Person") },
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

                // Due date quick buttons
                Text("Due: ${dateFormat.format(dueDate)}", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 0)
                        }
                        dueDate = cal.timeInMillis
                    }) {
                        Text("Today")
                    }
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 0)
                        }
                        dueDate = cal.timeInMillis
                    }) {
                        Text("Tomorrow")
                    }
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 7)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 0)
                        }
                        dueDate = cal.timeInMillis
                    }) {
                        Text("+7 Days")
                    }
                }
            }
        }
    }
}
