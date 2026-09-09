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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailView(
    item: AssignmentEntity,
    courses: List<CourseEntity>,
    database: AppDatabase,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    
    var content by remember { mutableStateOf(item.content) }
    var detail by remember { mutableStateOf(item.detail) }
    var submitMethod by remember { mutableStateOf(item.submitMethod) }
    var dueDate by remember { mutableStateOf(item.dueDate) }
    var isCompleted by remember { mutableStateOf(item.isCompleted) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("Assignment") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                actions = {
                    if (isEditing) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.assignmentDao().update(
                                        item.copy(
                                            content = content.trim(),
                                            detail = detail.trim(),
                                            submitMethod = submitMethod.trim(),
                                            dueDate = dueDate,
                                            isCompleted = isCompleted
                                        )
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = content.trim().isNotEmpty()
                        ) { Text("Save") }
                    } else {
                        TextButton(onClick = { isEditing = true }) { Text("Edit") }
                    }
                }
            )
            Column(modifier = Modifier.padding(AppTheme.Spacing.page)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = submitMethod,
                        onValueChange = { submitMethod = it },
                        label = { Text("Submit Method") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it },
                        label = { Text("Details") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Completed")
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = isCompleted, onCheckedChange = { isCompleted = it })
                    }
                } else {
                    Text(item.content, style = MaterialTheme.typography.titleLarge)
                    if (item.submitMethod.isNotEmpty()) {
                        Text(item.submitMethod, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.detail.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Details", style = MaterialTheme.typography.titleMedium)
                        Text(item.detail)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Due Date: ${item.dueDate}")
                    item.courseId?.let { courseId ->
                        courses.find { it.id == courseId }?.let { course ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Course: ${course.name}")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Status: ${if (item.isCompleted) "Completed" else "Pending"}")
                    
                    if (!item.sourceURL.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Source URL: ${item.sourceURL}", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
