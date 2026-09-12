package com.schedule.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseMeetingEntity
import com.schedule.app.data.models.CourseWithMeetings
import com.schedule.app.data.models.WeekPattern
import com.schedule.app.ui.components.*
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.util.UUID

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

    val isFormValid = name.trim().isNotEmpty() && meetingDrafts.isNotEmpty() && meetingDrafts.all { it.isValid }

    IosModalBottomSheet(onDismissRequest = onDismiss) {
        if (!isEditing) {
            // View Mode Header: 关闭 / 课程详情 / 编辑
            IosSheetHeader(
                title = "课程详情",
                leftActionText = "关闭",
                onLeftAction = onDismiss,
                rightActionText = "编辑",
                onRightAction = { isEditing = true }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Course Info Section
                IosFormSection(headerText = "课程信息") {
                    IosFormRow(
                        label = "课程名称",
                        value = courseWithMeetings.course.name,
                        showChevron = false
                    )
                    if (courseWithMeetings.course.teacher.isNotBlank()) {
                        IosFormDivider()
                        IosFormRow(
                            label = "授课教师",
                            value = courseWithMeetings.course.teacher,
                            showChevron = false
                        )
                    }
                    if (courseWithMeetings.course.classroom.isNotBlank()) {
                        IosFormDivider()
                        IosFormRow(
                            label = "上课教室",
                            value = courseWithMeetings.course.classroom,
                            showChevron = false
                        )
                    }
                }

                // Schedule Section
                IosFormSection(headerText = "课程安排") {
                    courseWithMeetings.meetings.forEachIndexed { index, meeting ->
                        if (index > 0) IosFormDivider()
                        IosFormRow(
                            label = "${weekdayLabel(meeting.weekday)} · 第 ${meeting.startPeriod}-${meeting.endPeriod} 节",
                            value = patternLabel(meeting.pattern),
                            showChevron = false
                        )
                    }
                }

                // Teaching Network Section
                IosFormSection(headerText = "教学网联动") {
                    IosFormRow(
                        label = "教学网作业与课件",
                        value = "查看 & 下载",
                        labelColor = AppTheme.colors.accent,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = AppTheme.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        showChevron = true,
                        onClick = { onOpenTeachingHub?.invoke(courseWithMeetings.course.id) }
                    )
                }

                // Appearance Section
                IosFormSection(headerText = "外观") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "课程颜色",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val color = courseWithMeetings.course.displayColor()
                        Box(
                            modifier = Modifier
                                .size(width = 32.dp, height = 20.dp)
                                .background(color, RoundedCornerShape(6.dp))
                                .border(0.5.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        )
                    }
                }

                // Actions Section: Destructive Delete Course
                IosFormSection {
                    IosFormRow(
                        label = "删除课程",
                        labelColor = MaterialTheme.colorScheme.error,
                        showChevron = false,
                        onClick = { showDeleteAlert = true }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        } else {
            // Edit Mode Header: 取消 / 编辑课程 / 存储
            IosSheetHeader(
                title = "编辑课程",
                leftActionText = "取消",
                onLeftAction = { isEditing = false },
                rightActionText = "存储",
                rightActionEnabled = isFormValid,
                onRightAction = {
                    coroutineScope.launch {
                        val updatedCourse = courseWithMeetings.course.copy(
                            name = name.trim(),
                            teacher = teacher.trim(),
                            classroom = classroom.trim(),
                            colorHex = courseColorHex
                        )
                        database.courseDao().update(updatedCourse)

                        val draftIds = meetingDrafts.map { it.id }.toSet()

                        // Delete removed meetings
                        courseWithMeetings.meetings.filter { it.id !in draftIds }.forEach {
                            database.courseMeetingDao().delete(it)
                        }

                        // Update or insert meetings
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
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Course Info Section
                IosFormSection(headerText = "课程信息") {
                    IosFormTextFieldRow(
                        label = "名称",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "必填，如 高等数学"
                    )
                    IosFormDivider()
                    IosFormTextFieldRow(
                        label = "教师",
                        value = teacher,
                        onValueChange = { teacher = it },
                        placeholder = "选填，如 张老师"
                    )
                    IosFormDivider()
                    IosFormTextFieldRow(
                        label = "教室",
                        value = classroom,
                        onValueChange = { classroom = it },
                        placeholder = "选填，如 一教 101"
                    )
                }

                // 2. Schedule Sections
                meetingDrafts.forEachIndexed { index, draft ->
                    val sessionTitle = if (meetingDrafts.size > 1) "上课时段 ${index + 1}" else "上课时段"
                    IosFormSection(headerText = sessionTitle) {
                        if (meetingDrafts.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "时段 ${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        meetingDrafts = meetingDrafts.filter { it.id != draft.id }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除时段",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IosFormDivider()
                        }

                        // Weekday Picker Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "上课星期",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            IosWeekdayPillPicker(
                                selectedDay = draft.weekday,
                                onSelectDay = { day ->
                                    meetingDrafts = meetingDrafts.map { if (it.id == draft.id) it.copy(weekday = day) else it }
                                }
                            )
                        }

                        IosFormDivider()

                        // Period Range Row
                        IosPeriodRangeRow(
                            startPeriod = draft.startPeriod,
                            endPeriod = draft.endPeriod,
                            onRangeChanged = { newStart, newEnd ->
                                meetingDrafts = meetingDrafts.map {
                                    if (it.id == draft.id) it.copy(startPeriod = newStart, endPeriod = newEnd) else it
                                }
                            }
                        )

                        IosFormDivider()

                        // Week Pattern Picker Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "单双周",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AppSegmentedPicker(
                                selection = draft.weekPattern,
                                onSelectionChange = { pattern ->
                                    meetingDrafts = meetingDrafts.map { if (it.id == draft.id) it.copy(weekPattern = pattern) else it }
                                },
                                options = WeekPattern.entries,
                                title = { patternLabel(it) }
                            )
                        }
                    }
                }

                // Add Session Row
                IosFormSection {
                    IosFormRow(
                        label = "+ 添加上课时段",
                        labelColor = AppTheme.colors.accent,
                        showChevron = false,
                        onClick = {
                            meetingDrafts = meetingDrafts + MeetingDraft()
                        }
                    )
                }

                // 3. Course Color Section
                IosFormSection(headerText = "课程颜色") {
                    IosCourseColorPickerRow(
                        selectedColorHex = courseColorHex,
                        onColorSelected = { courseColorHex = it }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Delete Course Confirmation Alert
        if (showDeleteAlert) {
            AlertDialog(
                onDismissRequest = { showDeleteAlert = false },
                title = { Text("删除课程？", fontWeight = FontWeight.Bold) },
                text = { Text("此操作将同时移除该课程以及所有相关的作业和考试记录，无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                database.courseDao().delete(courseWithMeetings.course)
                                showDeleteAlert = false
                                onDismiss()
                            }
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAlert = false }) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = AppTheme.colors.surface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

