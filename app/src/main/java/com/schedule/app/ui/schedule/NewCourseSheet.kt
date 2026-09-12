package com.schedule.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.CourseMeetingEntity
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.data.models.WeekPattern
import com.schedule.app.ui.components.*
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.util.UUID

data class MeetingDraft(
    val id: String = UUID.randomUUID().toString(),
    var weekday: Int = 1,
    var startPeriod: Int = 1,
    var endPeriod: Int = 2,
    var weekPattern: WeekPattern = WeekPattern.ALL
) {
    val isValid: Boolean get() = weekday in 1..7 && startPeriod in 1..12 && endPeriod >= startPeriod && endPeriod <= 12
}

fun weekdayLabel(day: Int): String {
    return when (day) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }
}

fun patternLabel(pattern: WeekPattern): String {
    return when (pattern) {
        WeekPattern.ALL -> "全周"
        WeekPattern.ODD -> "单周"
        WeekPattern.EVEN -> "双周"
    }
}

val coursePresetColors = listOf("", "#DBF5E6", "#CCEBD8", "#E6FAF0", "#C7E6CC", "#EBF5E0", "#D6F0EB")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCourseSheet(
    term: SettingEntity,
    database: AppDatabase,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var courseColorHex by remember { mutableStateOf("") }
    var meetingDrafts by remember { mutableStateOf(listOf(MeetingDraft())) }

    val isFormValid = name.trim().isNotEmpty() && meetingDrafts.isNotEmpty() && meetingDrafts.all { it.isValid }

    IosModalBottomSheet(onDismissRequest = onDismiss) {
        IosSheetHeader(
            title = "新建课程",
            leftActionText = "取消",
            onLeftAction = onDismiss,
            rightActionText = "存储",
            rightActionEnabled = isFormValid,
            onRightAction = {
                coroutineScope.launch {
                    val newCourse = CourseEntity(
                        id = UUID.randomUUID().toString(),
                        termId = term.id,
                        name = name.trim(),
                        teacher = teacher.trim(),
                        classroom = classroom.trim(),
                        colorHex = courseColorHex,
                        createdAt = System.currentTimeMillis()
                    )
                    database.courseDao().insert(newCourse)

                    meetingDrafts.forEach { draft ->
                        val meeting = CourseMeetingEntity(
                            id = UUID.randomUUID().toString(),
                            courseId = newCourse.id,
                            weekday = draft.weekday,
                            startPeriod = draft.startPeriod,
                            endPeriod = draft.endPeriod,
                            weekPattern = draft.weekPattern.name
                        )
                        database.courseMeetingDao().insert(meeting)
                    }
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
            // 1. Course Information Section
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

            // 2. Schedule Sections for each session
            meetingDrafts.forEachIndexed { index, draft ->
                val sessionTitle = if (meetingDrafts.size > 1) "上课时段 ${index + 1}" else "上课时段"
                IosFormSection(headerText = sessionTitle) {
                    // Session Header row with optional delete
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

                    // Weekday Selector Row
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

            // Add Session Button Row
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

            // 3. Course Color / Appearance Section
            IosFormSection(headerText = "课程颜色") {
                IosCourseColorPickerRow(
                    selectedColorHex = courseColorHex,
                    onColorSelected = { courseColorHex = it }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Native iOS 7-day pill row selector (周一 至 周日)
 */
@Composable
fun IosWeekdayPillPicker(
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        (1..7).forEach { day ->
            val isSelected = selectedDay == day
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) AppTheme.colors.accent else AppTheme.colors.controlFill,
                shadowElevation = 0.dp,
                onClick = { onSelectDay(day) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = weekdayLabel(day),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Native iOS Period Range selector with standard presets and capsule stepper
 */
@Composable
fun IosPeriodRangeRow(
    startPeriod: Int,
    endPeriod: Int,
    onRangeChanged: (start: Int, end: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = remember {
        listOf(
            Pair(1, 2),
            Pair(3, 4),
            Pair(5, 6),
            Pair(7, 8),
            Pair(7, 9),
            Pair(10, 11),
            Pair(10, 12)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "节次范围",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            // iOS Capsule Stepper for fine-tuning
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 26.dp)
                            .iosPressable {
                                if (endPeriod > startPeriod) onRangeChanged(startPeriod, endPeriod - 1)
                                else if (startPeriod > 1) onRangeChanged(startPeriod - 1, endPeriod - 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.accent)
                    }
                    Text(
                        text = "第 $startPeriod-$endPeriod 节",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 26.dp)
                            .iosPressable {
                                if (endPeriod < 12) onRangeChanged(startPeriod, endPeriod + 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.accent)
                    }
                }
            }
        }

        // Preset Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { (start, end) ->
                val isSelected = startPeriod == start && endPeriod == end
                val label = "$start-$end 节"
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) AppTheme.colors.selectedFill else AppTheme.colors.controlFill,
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    onClick = { onRangeChanged(start, end) }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS Course Color Picker row with "自动 (柔和绿)" and 6 Morandi color swatches
 */
@Composable
fun IosCourseColorPickerRow(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Auto (Adaptive Theme Green) option
        val isAuto = selectedColorHex.isEmpty()
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isAuto) AppTheme.colors.selectedFill else AppTheme.colors.controlFill,
            border = if (isAuto) androidx.compose.foundation.BorderStroke(1.2.dp, AppTheme.colors.accent) else null,
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp)),
            onClick = { onColorSelected("") }
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "自动 (柔和绿)",
                    fontSize = 13.sp,
                    fontWeight = if (isAuto) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isAuto) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Preset Morandi Pastel Colors
        coursePresetColors.filter { it.isNotEmpty() }.forEach { hex ->
            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { AppTheme.colors.highlight }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 0.5.dp,
                        color = if (isSelected) AppTheme.colors.accent else Color.Black.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .iosPressable { onColorSelected(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "已选择",
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

