package com.schedule.app.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.ExamEntity
import com.schedule.app.ui.components.*
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    
    // Default exam date: 14 days from now, 09:00
    val defaultCal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 14)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var examDate by remember { mutableStateOf(defaultCal.timeInMillis) }
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var expandedCourseDropdown by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA) }

    fun showDateTimePicker() {
        val currentCal = Calendar.getInstance().apply { timeInMillis = examDate }
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
                        examDate = newCal.timeInMillis
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
            title = "新建考试日程",
            leftActionText = "取消",
            onLeftAction = onDismiss,
            rightActionText = "保存",
            onRightAction = {
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
            rightActionEnabled = subject.isNotBlank()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IosFormSection(headerText = "考试信息") {
                IosFormTextFieldRow(
                    label = "考试科目",
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = "例如：期中考试 / 微积分"
                )
                IosFormDivider()
                IosFormTextFieldRow(
                    label = "地点/备注",
                    value = detail,
                    onValueChange = { detail = it },
                    placeholder = "例如：一教 101，带学生证",
                    minLines = 2
                )
            }

            IosFormSection(headerText = "课程关联") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val selectedName = courses.find { it.id == selectedCourseId }?.name ?: "未关联课程"
                    IosFormRow(
                        label = "所属课程",
                        value = selectedName,
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

            IosFormSection(headerText = "考试时间") {
                IosFormRow(
                    label = "考试时间",
                    value = dateFormat.format(examDate),
                    valueColor = AppTheme.colors.accent,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = AppTheme.colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { showDateTimePicker() }
                )
            }

            // Quick date shortcut buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "+1 周" to {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 7)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        examDate = cal.timeInMillis
                    },
                    "+2 周" to {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 14)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        examDate = cal.timeInMillis
                    },
                    "+1 个月" to {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 30)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        examDate = cal.timeInMillis
                    }
                ).forEach { (label, action) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .iosPressable(onClick = action),
                        shape = RoundedCornerShape(8.dp),
                        color = AppTheme.colors.surface,
                        border = BorderStroke(0.5.dp, AppTheme.colors.border.copy(alpha = 0.5f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = AppTheme.colors.accent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
