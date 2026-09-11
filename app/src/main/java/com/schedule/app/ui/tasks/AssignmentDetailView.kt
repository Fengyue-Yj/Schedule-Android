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
import com.schedule.app.util.DateFormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailView(
    item: AssignmentEntity,
    courses: List<CourseEntity>,
    database: AppDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    
    var content by remember { mutableStateOf(item.content) }
    var detail by remember { mutableStateOf(item.detail) }
    var submitMethod by remember { mutableStateOf(item.submitMethod) }
    var dueDate by remember { mutableStateOf(item.dueDate) }
    var isCompleted by remember { mutableStateOf(item.isCompleted) }

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
                title = { Text(if (isEditing) "编辑作业" else "待办作业详情") },
                navigationIcon = {
                    TextButton(onClick = {
                        if (isEditing) isEditing = false else onDismiss()
                    }) { Text(if (isEditing) "取消编辑" else "关闭") }
                },
                actions = {
                    if (isEditing) {
                        Button(
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
                                    isEditing = false
                                }
                            },
                            enabled = content.trim().isNotEmpty()
                        ) { Text("保存") }
                    } else {
                        TextButton(onClick = { isEditing = true }) { Text("编辑") }
                    }
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AppTheme.Spacing.page),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("作业标题 / 待办内容") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = submitMethod,
                        onValueChange = { submitMethod = it },
                        label = { Text("提交方式") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = detail,
                        onValueChange = { detail = it },
                        label = { Text("详细备注") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("标记为已完成", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(checked = isCompleted, onCheckedChange = { isCompleted = it })
                    }
                } else {
                    Text(content, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    if (submitMethod.isNotEmpty()) {
                        Text("提交方式: $submitMethod", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "截止时间 (DDL): ${DateFormatUtil.formatDateTime(dueDate)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    item.courseId?.let { courseId ->
                        courses.find { it.id == courseId }?.let { course ->
                            Text("所属课程: ${course.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Text(
                        text = if (isCompleted) "状态: 已完成" else "状态: 待完成",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )

                    if (detail.isNotEmpty()) {
                        Divider()
                        Text("详细说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(detail, style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    if (!item.sourceURL.isNullOrEmpty()) {
                        Divider()
                        Text("教学网来源链接: ${item.sourceURL}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
