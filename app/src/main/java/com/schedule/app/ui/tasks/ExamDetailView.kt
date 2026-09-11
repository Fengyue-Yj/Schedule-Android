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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.ExamEntity
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
fun ExamDetailView(
    item: ExamEntity,
    courses: List<CourseEntity>,
    database: AppDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    
    var subject by remember { mutableStateOf(item.subject) }
    var detail by remember { mutableStateOf(item.detail) }
    var date by remember { mutableStateOf(item.date) }
    var selectedCourseId by remember { mutableStateOf(item.courseId) }
    var expandedCourseDropdown by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) }

    fun showDateTimePicker() {
        val currentCal = Calendar.getInstance().apply { timeInMillis = date }
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
                        date = newCal.timeInMillis
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
            title = if (isEditing) "编辑考试" else "考试详情",
            leftActionText = if (isEditing) "取消" else "关闭",
            onLeftAction = {
                if (isEditing) {
                    subject = item.subject
                    detail = item.detail
                    date = item.date
                    selectedCourseId = item.courseId
                    isEditing = false
                } else {
                    onDismiss()
                }
            },
            rightActionText = if (isEditing) "保存" else "编辑",
            onRightAction = {
                if (isEditing) {
                    coroutineScope.launch(Dispatchers.IO) {
                        database.examDao().update(
                            item.copy(
                                subject = subject.trim(),
                                detail = detail.trim(),
                                date = date,
                                courseId = selectedCourseId
                            )
                        )
                        isEditing = false
                    }
                } else {
                    isEditing = true
                }
            },
            rightActionEnabled = !isEditing || subject.isNotBlank()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing) {
                IosFormSection(headerText = "考试信息") {
                    IosFormTextFieldRow(
                        label = "考试科目",
                        value = subject,
                        onValueChange = { subject = it },
                        placeholder = "输入科目名称"
                    )
                    IosFormDivider()
                    IosFormTextFieldRow(
                        label = "地点/备注",
                        value = detail,
                        onValueChange = { detail = it },
                        placeholder = "例如：二教 301 / 闭卷",
                        minLines = 2
                    )
                }

                IosFormSection(headerText = "课程关联") {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val courseName = courses.find { it.id == selectedCourseId }?.name ?: "未关联课程"
                        IosFormRow(
                            label = "所属课程",
                            value = courseName,
                            onClick = { expandedCourseDropdown = true }
                        )
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
                }

                IosFormSection(headerText = "时间安排") {
                    IosFormRow(
                        label = "考试时间",
                        value = dateFormat.format(date),
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
                }
            } else {
                IosFormSection {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
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
                        label = "考试时间",
                        value = DateFormatUtil.formatDateTime(date),
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
                }

                if (detail.isNotBlank()) {
                    IosFormSection(headerText = "地点与注意事项") {
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
