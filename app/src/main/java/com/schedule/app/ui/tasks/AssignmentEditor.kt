package com.schedule.app.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var submitMethod by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    
    // Default due date: today 23:59
    val defaultCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var dueDate by remember { mutableStateOf(defaultCal.timeInMillis) }
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var expandedCourseDropdown by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) }

    fun showDateTimePicker() {
        val currentCal = Calendar.getInstance().apply { timeInMillis = dueDate }
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val newCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        dueDate = newCal.timeInMillis
                    },
                    currentCal.get(Calendar.HOUR_OF_DAY),
                    currentCal.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH),
            currentCal.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("新建待办作业") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("取消") }
                },
                actions = {
                    Button(
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
                                        detail = detail.trim(),
                                        sourceURL = null
                                    )
                                )
                                onSave()
                                onDismiss()
                            }
                        },
                        enabled = content.trim().isNotEmpty()
                    ) { Text("保存") }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.Spacing.page),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("作业标题 / 待办内容") },
                    placeholder = { Text("例如：完成数学大作业、实验报告") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = submitMethod,
                    onValueChange = { submitMethod = it },
                    label = { Text("提交方式 (选填)") },
                    placeholder = { Text("例如：教学网 / 邮箱 / 纸质提交") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("详细备注 (选填)") },
                    placeholder = { Text("要求或注意事项...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Course selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedCourseDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedName = courses.find { it.id == selectedCourseId }?.name ?: "未关联课程 (点击选择)"
                        Text("所属课程: $selectedName")
                    }
                    DropdownMenu(
                        expanded = expandedCourseDropdown,
                        onDismissRequest = { expandedCourseDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("不关联课程") },
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

                // Due date card with custom picker
                Card(
                    onClick = { showDateTimePicker() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "截止时间 (DDL)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormat.format(dueDate),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        FilledTonalButton(onClick = { showDateTimePicker() }) {
                            Text("自选时间")
                        }
                    }
                }

                // Due date quick buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            dueDate = cal.timeInMillis
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("今天 23:59")
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            dueDate = cal.timeInMillis
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("明天 23:59")
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 7)
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            dueDate = cal.timeInMillis
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+7 天")
                    }
                }
            }
        }
    }
}
