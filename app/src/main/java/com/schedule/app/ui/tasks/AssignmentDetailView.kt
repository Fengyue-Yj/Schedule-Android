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
import com.schedule.app.ui.components.*
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

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        IosSheetHeader(
            title = if (isEditing) "编辑作业" else "待办作业详情",
            leftActionText = if (isEditing) "取消" else "关闭",
            onLeftAction = {
                if (isEditing) {
                    content = item.content
                    detail = item.detail
                    submitMethod = item.submitMethod
                    dueDate = item.dueDate
                    isCompleted = item.isCompleted
                    isEditing = false
                } else {
                    onDismiss()
                }
            },
            rightActionText = if (isEditing) "保存" else "编辑",
            onRightAction = {
                if (isEditing) {
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
                } else {
                    isEditing = true
                }
            },
            rightActionEnabled = !isEditing || content.isNotBlank()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing) {
                IosFormSection(headerText = "基本信息") {
                    IosFormTextFieldRow(
                        label = "作业标题",
                        value = content,
                        onValueChange = { content = it },
                        placeholder = "输入作业或待办内容"
                    )
                    IosFormDivider()
                    IosFormTextFieldRow(
                        label = "提交方式",
                        value = submitMethod,
                        onValueChange = { submitMethod = it },
                        placeholder = "选填，例如：教学网 / 邮箱"
                    )
                    IosFormDivider()
                    IosFormTextFieldRow(
                        label = "详细备注",
                        value = detail,
                        onValueChange = { detail = it },
                        placeholder = "要求或注意事项...",
                        minLines = 3
                    )
                }

                IosFormSection(headerText = "时间与状态") {
                    IosFormRow(
                        label = "截止时间 (DDL)",
                        value = dateFormat.format(dueDate),
                        valueColor = AppTheme.colors.accent,
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = AppTheme.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { showDateTimePicker() }
                    )
                    IosFormDivider()
                    IosFormSwitchRow(
                        label = "标记为已完成",
                        checked = isCompleted,
                        onCheckedChange = { isCompleted = it }
                    )
                }
            } else {
                IosFormSection {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (submitMethod.isNotBlank()) {
                        IosFormDivider()
                        IosFormRow(
                            label = "提交方式",
                            value = submitMethod,
                            showChevron = false
                        )
                    }

                    val course = courses.find { it.id == item.courseId }
                    if (course != null) {
                        IosFormDivider()
                        IosFormRow(
                            label = "所属课程",
                            value = course.name,
                            showChevron = false
                        )
                    }

                    IosFormDivider()
                    IosFormRow(
                        label = "截止时间 (DDL)",
                        value = DateFormatUtil.formatDateTime(dueDate),
                        valueColor = AppTheme.colors.accent,
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = AppTheme.colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        showChevron = false
                    )

                    IosFormDivider()
                    IosFormRow(
                        label = "状态",
                        value = if (isCompleted) "已完成" else "待完成",
                        valueColor = if (isCompleted) AppTheme.colors.accent else MaterialTheme.colorScheme.error,
                        showChevron = false
                    )
                }

                if (detail.isNotBlank()) {
                    IosFormSection(headerText = "详细备注") {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (!item.sourceURL.isNullOrBlank()) {
                    IosFormSection(headerText = "教学网来源") {
                        Text(
                            text = item.sourceURL,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.accent,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
