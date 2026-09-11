package com.schedule.app.ui.teaching

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseEntity
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.teaching.DownloadStatus
import com.schedule.app.teaching.TeachingDownloader
import com.schedule.app.teaching.TeachingImporter
import com.schedule.app.teaching.TeachingKind
import com.schedule.app.teaching.TeachingParser
import com.schedule.app.teaching.TeachingStore
import com.schedule.app.util.DateFormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

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
                // Auto fuzzy match course
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

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
            )
        },
        topBar = {
            TopAppBar(
                title = { Text(item?.displayCourseTitle ?: "详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (item?.sourceURL?.isNotBlank() == true) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.sourceURL))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "在浏览器中打开")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (item != null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and course
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.displayCourseTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Publication date
                val pubTime = item.publishedAt ?: item.publishedText?.let { TeachingParser.parseDate(it)?.time }
                if (pubTime != null || !item.publishedText.isNullOrBlank()) {
                    val pubFormatted = if (pubTime != null) DateFormatUtil.formatDateTime(pubTime) else item.publishedText?.replace("发布者:", "")?.trim()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "发布时间: $pubFormatted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Grade information card
                if (item.kind == TeachingKind.GRADE) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("成绩详情", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val scoreDisplay = item.score ?: item.gradeStatus ?: "未出分"
                                Text(
                                    text = scoreDisplay,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!item.pointsPossible.isNullOrBlank()) {
                                    Text(
                                        text = "满分: ${item.pointsPossible}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            if (!item.gradeCategory.isNullOrBlank()) {
                                Text("考核类别: ${item.gradeCategory}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!item.feedback.isNullOrBlank()) {
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                Text("💬 教师评语: ${item.feedback}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Due date card (for assignments, show interactive card allowing tap to modify or set)
                if (item.kind == TeachingKind.ASSIGNMENT) {
                    Card(
                        onClick = { showDateTimePicker() },
                        colors = CardDefaults.cardColors(
                            containerColor = if (customDueDate != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (customDueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (customDueDate != null) "截止时间 (DDL)" else "截止时间 (未识别/未设置)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (customDueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (customDueDate != null) DateFormatUtil.formatDateTime(customDueDate!!) else "点击此处添加截止时间 (DDL)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (customDueDate != null) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (customDueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = if (customDueDate != null) "修改" else "设置",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else if (item.dueDate != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "截止时间: ${DateFormatUtil.formatDateTime(item.dueDate)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Body content
                if (item.body.isNotBlank()) {
                    val sectionTitle = when (item.kind) {
                        TeachingKind.GRADE -> "成绩说明 / 反馈"
                        TeachingKind.ASSIGNMENT -> "作业说明"
                        TeachingKind.MATERIAL -> "资料说明"
                        TeachingKind.ANNOUNCEMENT -> "通知内容"
                    }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(sectionTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text(text = item.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Import Assignment Section
                if (item.kind == TeachingKind.ASSIGNMENT && term != null && database != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("添加到待办区", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (isImported) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "已在待办中",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            val matchedCourse = courses.find { it.id == selectedCourseId }
                            val courseLabel = if (matchedCourse != null) {
                                "所属课程: ${item.displayCourseTitle} (已自动关联本地课表: ${matchedCourse.name})"
                            } else {
                                "所属课程: ${item.displayCourseTitle}"
                            }
                            Text(
                                text = courseLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

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
                                        snackbarHostState.showSnackbar("已成功添加作业「${item.title}」至待办列表！")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isImported) "更新待办作业" else "一键加入待办")
                            }
                        }
                    }
                }

                // Attachments Section
                if (item.attachments.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "资料与课件附件 (${item.attachments.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            item.attachments.forEach { att ->
                                val localFile = TeachingDownloader.getDownloadedFile(context, att.name)
                                val downloadState = downloadStates[att.url] ?: DownloadStatus.Idle

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AttachFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                att.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (localFile != null || downloadState is DownloadStatus.Success) {
                                                Text(
                                                    "已下载至手机 · 可直接打开",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Action buttons
                                        if (downloadState is DownloadStatus.Downloading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else if (localFile != null || downloadState is DownloadStatus.Success) {
                                            val fileToUse = localFile ?: (downloadState as DownloadStatus.Success).file
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        if (!TeachingDownloader.openFile(context, fileToUse)) {
                                                            Toast.makeText(context, "未找到支持打开此格式的应用", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text("打开")
                                                }
                                                IconButton(
                                                    onClick = { TeachingDownloader.shareFile(context, fileToUse) }
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = "分享")
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
                                                            val msg = "「${att.name}」下载成功！已存至系统下载目录。"
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            snackbarHostState.showSnackbar(msg)
                                                        } else {
                                                            val msg = "下载失败: ${res.exceptionOrNull()?.message}"
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            snackbarHostState.showSnackbar(msg)
                                                        }
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Download,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("直接下载")
                                            }
                                        }
                                    }

                                    // Progress bar if downloading
                                    if (downloadState is DownloadStatus.Downloading) {
                                        if (downloadState.progress >= 0f) {
                                            LinearProgressIndicator(
                                                progress = { downloadState.progress },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                "下载中 ${(downloadState.progress * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            Text(
                                                "下载中...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(top = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
