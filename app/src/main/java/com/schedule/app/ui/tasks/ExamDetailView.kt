package com.schedule.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.ExamEntity
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailView(
    item: ExamEntity,
    courses: List<CourseEntity>,
    database: AppDatabase,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    
    var subject by remember { mutableStateOf(item.subject) }
    var detail by remember { mutableStateOf(item.detail) }
    var date by remember { mutableStateOf(item.date) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("Exam") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                actions = {
                    if (isEditing) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.examDao().update(
                                        item.copy(
                                            subject = subject.trim(),
                                            detail = detail.trim(),
                                            date = date
                                        )
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = subject.trim().isNotEmpty()
                        ) { Text("Save") }
                    } else {
                        TextButton(onClick = { isEditing = true }) { Text("Edit") }
                    }
                }
            )
            Column(modifier = Modifier.padding(AppTheme.Spacing.page)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it },
                        label = { Text("Detail") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // TODO DatePicker
                } else {
                    Text(item.subject, style = MaterialTheme.typography.titleLarge)
                    if (item.detail.isNotEmpty()) {
                        Text(item.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Date: ${item.date}")
                    item.courseId?.let { courseId ->
                        courses.find { it.id == courseId }?.let { course ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Course: ${course.name}")
                        }
                    }
                }
            }
        }
    }
}
