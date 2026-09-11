package com.schedule.app.ui.teaching

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.teaching.DownloadStatus
import com.schedule.app.teaching.TeachingDownloader
import com.schedule.app.teaching.TeachingImporter
import com.schedule.app.teaching.TeachingItem
import com.schedule.app.teaching.TeachingKind
import com.schedule.app.teaching.TeachingParser
import com.schedule.app.teaching.TeachingStore
import com.schedule.app.ui.components.*
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.util.DateFormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingItemView(
    itemId: String,
    term: SettingEntity? = null,
    database: AppDatabase? = null,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val store = remember { TeachingStore.getInstance(context) }
    val snapshot by store.snapshot.collectAsState()
    val item = snapshot.items.find { it.id == itemId }
    val scope = rememberCoroutineScope()
    val downloadStates by TeachingDownloader.downloadStates.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isImported by remember { mutableStateOf(false) }
    var courses by remember { mutableStateOf<List<CourseEntity>>(emptyList()) }
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var customDueDate by remember(item) { mutableStateOf(item?.dueDate) }
    var showCoursePickerSheet by remember { mutableStateOf(false) }
    var isDownloadingAll by remember { mutableStateOf(false) }

    fun showDateTimePicker() {
        val baseTime = customDueDate ?: item?.dueDate ?: System.currentTimeMillis()
        val currentCal = java.util.Calendar.getInstance().apply { timeInMillis = baseTime }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val newCal = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.YEAR, year)
                            set(java.util.Calendar.MONTH, month)
                            set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                            set(java.util.Calendar.MINUTE, minute)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        customDueDate = newCal.timeInMillis
                    },
                    currentCal.get(java.util.Calendar.HOUR_OF_DAY),
                    currentCal.get(java.util.Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            currentCal.get(java.util.Calendar.YEAR),
            currentCal.get(java.util.Calendar.MONTH),
            currentCal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    LaunchedEffect(itemId, term, database) {
        if (term != null && database != null && item != null) {
            val loadedCourses = withContext(Dispatchers.IO) {
                database.courseDao().getCoursesForTerm(term.id)
            }
            courses = loadedCourses

            // Check if already imported
            val existing = withContext(Dispatchers.IO) {
                database.assignmentDao().findBySourceId(item.id)
            }
            isImported = existing != null
            if (existing?.courseId != null) {
                selectedCourseId = existing.courseId
            } else {
                val matched = TeachingImporter.findMatchingCourse(item, loadedCourses)
                selectedCourseId = matched?.id
            }
            if (existing?.dueDate != null) {
                customDueDate = existing.dueDate
            }
            item.let {
                store.markRead(it.id)
                store.markRead(it.itemReadKey)
            }
        }
    }

    val navTitle = when (item?.kind) {
        TeachingKind.GRADE -> "成绩详情"
        TeachingKind.ASSIGNMENT -> "作业详情"
        TeachingKind.MATERIAL -> "资料课件"
        TeachingKind.ANNOUNCEMENT -> "通知详情"
        null -> "详情"
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
            )
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.colors.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left action: iOS "完成" button
                    Text(
                        text = "完成",
                        color = AppTheme.colors.accent,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        modifier = Modifier
                            .iosPressable(onClick = onNavigateBack)
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    )

                    // Center Title
                    Text(
                        text = navTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Right action: Open in browser
                    if (item?.sourceURL?.isNotBlank() == true) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceURL))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "在浏览器中打开",
                                tint = AppTheme.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(36.dp))
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AppTheme.colors.border.copy(alpha = 0.35f)
                )
            }
        }
    ) { padding ->
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("未找到相关内容", style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Basic Info & Details
            item {
                IosFormSection {
                    // Title and Course header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.displayCourseTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // For Assignment: Due Date Row (interactive DDL picker)
                    if (item.kind == TeachingKind.ASSIGNMENT) {
                        IosFormDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosPressable { showDateTimePicker() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = AppTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "截止时间 (DDL)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (customDueDate != null) DateFormatUtil.formatDateTime(customDueDate!!) else "未设置 (点击添加)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (customDueDate != null) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontWeight = if (customDueDate != null) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // For Grade: Score & Feedback display
                    if (item.kind == TeachingKind.GRADE) {
                        IosFormDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val hasScore = !item.score.isNullOrBlank()
                            val isPending = item.gradeStatus == "待评分" || item.score == "待评分"

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isPending) "待评分" else (item.score ?: "未出分"),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    ),
                                    color = if (isPending) MaterialTheme.colorScheme.onTertiaryContainer else AppTheme.colors.accent
                                )
                                if (!item.pointsPossible.isNullOrBlank()) {
                                    Text(
                                        text = "满分: ${item.pointsPossible}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                            }

                            if (!item.gradeCategory.isNullOrBlank()) {
                                Text(
                                    text = "考核类别: ${item.gradeCategory}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!item.feedback.isNullOrBlank()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    thickness = 0.5.dp,
                                    color = AppTheme.colors.border.copy(alpha = 0.35f)
                                )
                                Text(
                                    text = "💬 教师评语: ${item.feedback}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Publish time row if present
                    val pubTime = item.publishedAt ?: item.publishedText?.let { TeachingParser.parseDate(it)?.time }
                    if (pubTime != null || !item.publishedText.isNullOrBlank()) {
                        val pubFormatted = if (pubTime != null) DateFormatUtil.formatDateTime(pubTime) else item.publishedText?.replace("发布者:", "")?.trim()
                        IosFormDivider()
                        IosFormRow(
                            label = "发布时间",
                            value = pubFormatted,
                            showChevron = false
                        )
                    }

                    // Body content if present
                    if (item.body.isNotBlank()) {
                        IosFormDivider()
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.body,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Link to source
                    if (item.sourceURL.isNotBlank()) {
                        IosFormDivider()
                        IosFormRow(
                            label = "在浏览器中打开原网页",
                            value = null,
                            showChevron = true,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceURL))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            // Section 2: Destination & Import (For Assignment)
            if (item.kind == TeachingKind.ASSIGNMENT && term != null && database != null) {
                item {
                    val matchedCourse = courses.find { it.id == selectedCourseId }
                    val footerMsg = if (isImported) {
                        "已添加至当前学期「${term.displayName}」。重新导入将保留您的本地修改与完成状态。"
                    } else {
                        "导入至当前学期「${term.displayName}」待办列表。首次导入可关联本地课表课程。"
                    }

                    IosFormSection(
                        headerText = "待办事项导入",
                        footerText = footerMsg
                    ) {
                        // Local course selection row
                        IosFormRow(
                            label = "关联本地课程",
                            value = matchedCourse?.name ?: "未关联 (点击选择)",
                            showChevron = true,
                            onClick = { showCoursePickerSheet = true }
                        )

                        IosFormDivider()

                        // iOS-style Filled Action Button
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val selectedCourse = courses.find { it.id == selectedCourseId }
                                        TeachingImporter.apply(
                                            item = item,
                                            term = term,
                                            course = selectedCourse,
                                            chosenDate = customDueDate,
                                            database = database
                                        )
                                        isImported = true
                                        val msg = if (isImported) "已更新待办作业「${item.title}」" else "已成功添加作业「${item.title}」至待办列表！"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.colors.accent,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (isImported) "更新待办作业" else "导入至待办列表",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Course Files & Attachments (For Material or any item with attachments)
            if (item.attachments.isNotEmpty()) {
                item {
                    IosFormSection(
                        headerText = "课件与资料文件 (${item.attachments.size})"
                    ) {
                        item.attachments.forEachIndexed { index, att ->
                            if (index > 0) IosFormDivider()

                            val localFile = TeachingDownloader.getDownloadedFile(context, att.name)
                            val downloadState = downloadStates[att.url] ?: DownloadStatus.Idle

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = AppTheme.colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = att.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (localFile != null || downloadState is DownloadStatus.Success) {
                                            Text(
                                                text = "已下载至本地 · 可直接查看",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = AppTheme.colors.accent
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // Action buttons on the right
                                if (downloadState is DownloadStatus.Downloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = AppTheme.colors.accent
                                    )
                                } else if (localFile != null || downloadState is DownloadStatus.Success) {
                                    val fileToUse = localFile ?: (downloadState as DownloadStatus.Success).file
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        FilledTonalButton(
                                            onClick = {
                                                if (!TeachingDownloader.openFile(context, fileToUse)) {
                                                    Toast.makeText(context, "未找到支持打开此格式的应用", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("打开", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium))
                                        }
                                        IconButton(
                                            onClick = { TeachingDownloader.shareFile(context, fileToUse) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "分享",
                                                tint = AppTheme.colors.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = {
                                            scope.launch {
                                                val res = TeachingDownloader.download(
                                                    context = context,
                                                    url = att.url,
                                                    suggestedFileName = att.name,
                                                    referer = item.sourceURL
                                                )
                                                if (res.isSuccess) {
                                                    val msg = "「${att.name}」下载成功！"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    snackbarHostState.showSnackbar(msg)
                                                } else {
                                                    val msg = "下载失败: ${res.exceptionOrNull()?.message}"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("下载", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium))
                                    }
                                }
                            }
                        }

                        // Batch download if multiple files
                        if (item.attachments.size > 1) {
                            IosFormDivider()
                            Box(modifier = Modifier.padding(16.dp)) {
                                Button(
                                    onClick = {
                                        if (!isDownloadingAll) {
                                            isDownloadingAll = true
                                            scope.launch {
                                                try {
                                                    var succ = 0
                                                    var fail = 0
                                                    for (att in item.attachments) {
                                                        if (!TeachingDownloader.isDownloaded(context, att.name)) {
                                                            val res = TeachingDownloader.download(
                                                                context = context,
                                                                url = att.url,
                                                                suggestedFileName = att.name,
                                                                referer = item.sourceURL
                                                            )
                                                            if (res.isSuccess) succ++ else fail++
                                                        }
                                                    }
                                                    val msg = "全部下载完成: 成功 $succ 个${if (fail > 0) "，失败 $fail 个" else ""}"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    snackbarHostState.showSnackbar(msg)
                                                } finally {
                                                    isDownloadingAll = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isDownloadingAll,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppTheme.colors.accent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (isDownloadingAll) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("正在批量下载...", style = MaterialTheme.typography.bodyMedium)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("下载全部附件 (${item.attachments.size})", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for choosing local course to link
    if (showCoursePickerSheet) {
        IosModalBottomSheet(
            onDismissRequest = { showCoursePickerSheet = false }
        ) {
            IosSheetHeader(
                title = "关联本地课程",
                leftActionText = "完成",
                onLeftAction = { showCoursePickerSheet = false }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    IosFormSection {
                        // "未关联" option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosPressable {
                                    selectedCourseId = null
                                    showCoursePickerSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "不关联本地课程",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (selectedCourseId == null) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (selectedCourseId == null) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedCourseId == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Local courses list
                        courses.forEach { course ->
                            IosFormDivider()
                            val isSelected = selectedCourseId == course.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .iosPressable {
                                        selectedCourseId = course.id
                                        showCoursePickerSheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (isSelected) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AppTheme.colors.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
