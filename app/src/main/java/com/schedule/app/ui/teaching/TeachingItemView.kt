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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
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
import com.schedule.app.teaching.TeachingStore
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
    var expandedCourseMenu by remember { mutableStateOf(false) }

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

                // Due date
                if (item.dueDate != null) {
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
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
                            Text(
                                text = "截止时间: ${format.format(item.dueDate)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Body content
                if (item.body.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("内容说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                                Text("导入至待办作业", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

                            // Course Link Picker
                            Box {
                                OutlinedButton(
                                    onClick = { expandedCourseMenu = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val matchedCourse = courses.find { it.id == selectedCourseId }
                                    val text = if (matchedCourse != null) "关联课程: ${matchedCourse.name}" else "关联课程: 未关联 (点击选择)"
                                    Text(text)
                                }
                                DropdownMenu(
                                    expanded = expandedCourseMenu,
                                    onDismissRequest = { expandedCourseMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("不关联课程") },
                                        onClick = {
                                            selectedCourseId = null
                                            expandedCourseMenu = false
                                        }
                                    )
                                    courses.forEach { course ->
                                        DropdownMenuItem(
                                            text = { Text(course.name) },
                                            onClick = {
                                                selectedCourseId = course.id
                                                expandedCourseMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        val selectedCourse = courses.find { it.id == selectedCourseId }
                                        TeachingImporter.apply(
                                            item = item,
                                            term = term,
                                            course = selectedCourse,
                                            database = database
                                        )
                                        isImported = true
                                        snackbarHostState.showSnackbar("已成功导入作业「${item.title}」至待办列表！")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isImported) "更新待办作业" else "一键导入作业")
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
                                                        val res = TeachingDownloader.download(context, att.url, att.name)
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
