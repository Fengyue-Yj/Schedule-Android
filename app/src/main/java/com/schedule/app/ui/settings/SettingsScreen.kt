package com.schedule.app.ui.settings

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.data.models.TermSeason
import com.schedule.app.ui.components.*
import com.schedule.app.ui.teaching.TeachingHubScreen
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.util.CalendarManager
import com.schedule.app.util.CourseImporter
import com.schedule.app.util.TermDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    term: SettingEntity,
    database: AppDatabase,
    onDismiss: () -> Unit,
    onTermChanged: (SettingEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTerms by database.settingDao().getAll().collectAsState(initial = listOf(term))

    var currentDraft by remember(term) { mutableStateOf(TermDraft(term)) }
    var showSwitchSemesterSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showManageSemestersSheet by remember { mutableStateOf(false) }
    var termToEdit by remember { mutableStateOf<SettingEntity?>(null) }
    var termToDelete by remember { mutableStateOf<SettingEntity?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showTeachingHub by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateFormatChinese = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINA) }

    // CSV File Picker
    val csvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            CalendarManager.decodeCsvBytes(bytes)
                        } ?: ""
                    }
                    if (content.isNotBlank()) {
                        val summary = CourseImporter.importCSV(
                            csv = content,
                            termId = term.id,
                            courseDao = database.courseDao(),
                            meetingDao = database.courseMeetingDao()
                        )
                        Toast.makeText(
                            context,
                            summary.message,
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(context, "所选文件内容为空，请重新选择", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "导入失败: ${e.localizedMessage ?: "文件解析错误"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showTeachingHub) {
        TeachingHubScreen(
            onNavigateBack = { showTeachingHub = false },
            term = term,
            database = database
        )
        return
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(AppTheme.colors.background)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "取消",
                        color = AppTheme.colors.accent,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                        modifier = Modifier
                            .iosPressable(onClick = onDismiss)
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    )

                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "完成",
                        color = AppTheme.colors.accent,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        modifier = Modifier
                            .iosPressable(onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val updated = currentDraft.apply(term)
                                    database.settingDao().update(updated)
                                    withContext(Dispatchers.Main) {
                                        onTermChanged(updated)
                                        onDismiss()
                                    }
                                }
                            })
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AppTheme.colors.border.copy(alpha = 0.35f)
                )
            }
        },
        containerColor = AppTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. SEMESTERS Section
            IosFormSection(
                headerText = "Semesters",
                footerText = "各学期的课表、作业、考试与设置互相独立沙盒隔离。切换学期将放弃下方未保存的修改。"
            ) {
                IosFormRow(
                    label = "当前学期",
                    value = term.displayName,
                    onClick = { showSwitchSemesterSheet = true }
                )
                IosFormDivider()
                IosFormRow(
                    label = "+ 创建新学期",
                    labelColor = AppTheme.colors.accent,
                    showChevron = false,
                    onClick = { showCreateSheet = true }
                )
                IosFormDivider()
                IosFormRow(
                    label = "管理全部学期",
                    onClick = { showManageSemestersSheet = true }
                )
            }

            // 2. SEMESTER DETAILS Section
            IosFormSection(headerText = "Semester Details") {
                IosFormTextFieldRow(
                    label = "学期名称",
                    value = currentDraft.name,
                    onValueChange = { currentDraft = currentDraft.copy(name = it) },
                    placeholder = term.displayName
                )
                IosFormDivider()
                IosFormRow(
                    label = "第 1 周开学时间",
                    value = dateFormatChinese.format(currentDraft.startDate),
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = currentDraft.startDate }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selCal = Calendar.getInstance().apply {
                                    set(year, month, dayOfMonth, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val monday = CalendarManager.mondayOnOrBefore(selCal.timeInMillis)
                                currentDraft = currentDraft.copy(startDate = monday)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                )
                IosFormDivider()
                // Stepper Row: 学期总周数
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "学期总周数",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${currentDraft.totalWeeks} 周",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, AppTheme.colors.border.copy(alpha = 0.35f))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .iosPressable {
                                            if (currentDraft.totalWeeks > 1) {
                                                currentDraft = currentDraft.copy(totalWeeks = currentDraft.totalWeeks - 1)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.accent)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(0.5.dp)
                                        .height(20.dp)
                                        .background(AppTheme.colors.border.copy(alpha = 0.4f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .iosPressable {
                                            if (currentDraft.totalWeeks < 52) {
                                                currentDraft = currentDraft.copy(totalWeeks = currentDraft.totalWeeks + 1)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.accent)
                                }
                            }
                        }
                    }
                }
                IosFormDivider()
                IosFormTextFieldRow(
                    label = "年级",
                    value = currentDraft.grade,
                    onValueChange = { currentDraft = currentDraft.copy(grade = it) },
                    placeholder = "如: 大二 / 2024级"
                )
                IosFormDivider()
                IosFormRow(
                    label = "季节",
                    value = currentDraft.season.displayName,
                    onClick = {
                        currentDraft = currentDraft.copy(
                            season = if (currentDraft.season == TermSeason.FALL) TermSeason.SPRING else TermSeason.FALL
                        )
                    }
                )
            }

            // 3. SCHEDULE VIEW Section
            IosFormSection(headerText = "Schedule View") {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    AppSegmentedPicker(
                        label = "课表显示模式",
                        selection = currentDraft.showWeekends,
                        options = listOf(true, false),
                        title = { if (it) "显示周末" else "仅工作日" },
                        onSelectionChange = { currentDraft = currentDraft.copy(showWeekends = it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. DATA & APPEARANCE Section
            IosFormSection(headerText = "Data & Appearance") {
                IosFormRow(
                    label = "导入 CSV 格式课表",
                    onClick = { csvPicker.launch("*/*") }
                )
                IosFormDivider()
                IosFormRow(
                    label = "重新随机课程颜色",
                    showChevron = false,
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val courses = database.courseDao().getCoursesForTerm(term.id)
                            courses.forEach { course ->
                                database.courseDao().update(course.copy(colorHex = "", colorSeed = (1..1000000).random()))
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "已重新随机生成 ${courses.size} 门课程颜色", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                IosFormDivider()
                IosFormRow(
                    label = "北大教学网",
                    onClick = { showTeachingHub = true }
                )
            }

            // 5. DATA (Destructive) Section
            IosFormSection(
                headerText = "Data",
                footerText = "导入课表、随机颜色与清空数据将立即生效，其他学期数据不受影响。"
            ) {
                IosFormRow(
                    label = "清空当前学期数据",
                    labelColor = MaterialTheme.colorScheme.error,
                    showChevron = false,
                    onClick = { showClearDialog = true }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Switch Semester Sheet
    if (showSwitchSemesterSheet) {
        IosModalBottomSheet(
            onDismissRequest = { showSwitchSemesterSheet = false }
        ) {
            IosSheetHeader(
                title = "切换学期",
                leftActionText = "关闭",
                onLeftAction = { showSwitchSemesterSheet = false }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IosFormSection {
                    allTerms.forEachIndexed { index, item ->
                        if (index > 0) IosFormDivider()
                        IosFormRow(
                            label = item.displayName,
                            leadingIcon = if (item.id == term.id) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "当前选中",
                                        tint = AppTheme.colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            value = "${dateFormat.format(item.termStartDate)} · ${item.totalWeeks}周",
                            showChevron = false,
                            onClick = {
                                onTermChanged(item)
                                currentDraft = TermDraft(item)
                                showSwitchSemesterSheet = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Manage Semesters Sheet
    if (showManageSemestersSheet) {
        IosModalBottomSheet(
            onDismissRequest = { showManageSemestersSheet = false }
        ) {
            IosSheetHeader(
                title = "全部学期",
                leftActionText = "完成",
                onLeftAction = { showManageSemestersSheet = false },
                rightActionText = "+ 新建",
                onRightAction = { showCreateSheet = true }
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IosFormSection {
                    allTerms.forEachIndexed { index, item ->
                        if (index > 0) IosFormDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosPressable {
                                    onTermChanged(item)
                                    currentDraft = TermDraft(item)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (item.id == term.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "当前学期",
                                        tint = AppTheme.colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(18.dp))
                                }
                                Column {
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "第1周: ${dateFormat.format(item.termStartDate)} · 共${item.totalWeeks}周 · ${TermSeason.fromString(item.termSeason).displayName}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(onClick = { termToEdit = item }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "编辑",
                                        tint = AppTheme.colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (allTerms.size > 1) {
                                    IconButton(onClick = { termToDelete = item }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Term Editor Sheet (Create or Edit)
    val editingTarget = termToEdit
    if (showCreateSheet || editingTarget != null) {
        val isNew = showCreateSheet
        var editName by remember(isNew, editingTarget) {
            mutableStateOf(if (isNew) "" else (editingTarget?.name ?: ""))
        }
        var editWeeks by remember(isNew, editingTarget) {
            mutableIntStateOf(if (isNew) 20 else (editingTarget?.totalWeeks ?: 20))
        }
        var editSeason by remember(isNew, editingTarget) {
            mutableStateOf(
                if (isNew) {
                    if (Calendar.getInstance().get(Calendar.MONTH) >= Calendar.JULY) TermSeason.FALL else TermSeason.SPRING
                } else {
                    try { TermSeason.valueOf(editingTarget!!.termSeason) } catch(e: Exception) { TermSeason.FALL }
                }
            )
        }
        var editStartDate by remember(isNew, editingTarget) {
            mutableLongStateOf(
                if (isNew) CalendarManager.mondayOnOrBefore(System.currentTimeMillis())
                else (editingTarget?.termStartDate ?: System.currentTimeMillis())
            )
        }
        var editGrade by remember(isNew, editingTarget) {
            mutableStateOf(if (isNew) "" else (editingTarget?.grade ?: ""))
        }

        IosModalBottomSheet(
            onDismissRequest = {
                showCreateSheet = false
                termToEdit = null
            }
        ) {
            IosSheetHeader(
                title = if (isNew) "新建学期" else "编辑学期",
                leftActionText = "取消",
                onLeftAction = {
                    showCreateSheet = false
                    termToEdit = null
                },
                rightActionText = "保存",
                rightActionEnabled = editName.trim().isNotEmpty(),
                onRightAction = {
                    coroutineScope.launch(Dispatchers.IO) {
                        if (isNew) {
                            val draft = TermDraft().copy(
                                name = editName.trim(),
                                season = editSeason,
                                totalWeeks = editWeeks,
                                startDate = CalendarManager.mondayOnOrBefore(editStartDate),
                                grade = editGrade.trim()
                            )
                            val newSetting = draft.makeSetting()
                            database.settingDao().insert(newSetting)
                            withContext(Dispatchers.Main) {
                                onTermChanged(newSetting)
                                currentDraft = TermDraft(newSetting)
                                showCreateSheet = false
                            }
                        } else if (editingTarget != null) {
                            val updated = editingTarget.copy(
                                name = editName.trim(),
                                termSeason = editSeason.name,
                                totalWeeks = editWeeks,
                                termStartDate = CalendarManager.mondayOnOrBefore(editStartDate),
                                grade = editGrade.trim()
                            )
                            database.settingDao().update(updated)
                            withContext(Dispatchers.Main) {
                                if (editingTarget.id == term.id) {
                                    onTermChanged(updated)
                                    currentDraft = TermDraft(updated)
                                }
                                termToEdit = null
                            }
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IosFormSection {
                    IosFormTextFieldRow(
                        label = "学期名称",
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = "如: 2026 秋季学期"
                    )
                    IosFormDivider()
                    IosFormRow(
                        label = "季节",
                        value = editSeason.displayName,
                        onClick = {
                            editSeason = if (editSeason == TermSeason.FALL) TermSeason.SPRING else TermSeason.FALL
                        }
                    )
                    IosFormDivider()
                    IosFormRow(
                        label = "第 1 周开学时间",
                        value = dateFormatChinese.format(editStartDate),
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = editStartDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selCal = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    editStartDate = CalendarManager.mondayOnOrBefore(selCal.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                    IosFormDivider()
                    // Stepper for total weeks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "学期总周数",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "$editWeeks 周",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, AppTheme.colors.border.copy(alpha = 0.35f))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .iosPressable { if (editWeeks > 1) editWeeks-- },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.accent)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(0.5.dp)
                                            .height(20.dp)
                                            .background(AppTheme.colors.border.copy(alpha = 0.4f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .iosPressable { if (editWeeks < 52) editWeeks++ },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.accent)
                                    }
                                }
                            }
                        }
                    }
                    IosFormDivider()
                    IosFormTextFieldRow(
                        label = "年级",
                        value = editGrade,
                        onValueChange = { editGrade = it },
                        placeholder = "如: 大二 / 2024级"
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Delete Semester Confirmation Dialog
    termToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { termToDelete = null },
            title = { Text("删除学期？") },
            text = { Text("确定删除「${target.displayName}」及其所有课表、作业、考试和计划数据吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val isDeletingCurrent = target.id == term.id
                        val replacement = allTerms.firstOrNull { it.id != target.id }
                        coroutineScope.launch(Dispatchers.IO) {
                            database.courseDao().deleteByTermId(target.id)
                            database.assignmentDao().deleteByTermId(target.id)
                            database.examDao().deleteByTermId(target.id)
                            database.flexiblePlanDao().deleteByTermId(target.id)
                            database.settingDao().delete(target)
                            withContext(Dispatchers.Main) {
                                if (isDeletingCurrent && replacement != null) {
                                    onTermChanged(replacement)
                                    currentDraft = TermDraft(replacement)
                                }
                                termToDelete = null
                            }
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { termToDelete = null }) { Text("取消") }
            }
        )
    }

    // Clear Current Semester Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空当前学期数据？") },
            text = { Text("确定清空「${term.displayName}」下的所有课程、待办作业和考试日程吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            database.courseDao().deleteByTermId(term.id)
                            database.assignmentDao().deleteByTermId(term.id)
                            database.examDao().deleteByTermId(term.id)
                            withContext(Dispatchers.Main) {
                                showClearDialog = false
                                Toast.makeText(context, "已清空「${term.displayName}」的全部数据", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}
