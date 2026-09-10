package com.schedule.app.ui.teaching

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.schedule.app.ui.theme.AppTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
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
import com.schedule.app.teaching.TeachingItem
import com.schedule.app.teaching.TeachingKind
import com.schedule.app.teaching.TeachingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingHubScreen(
    onNavigateBack: () -> Unit,
    term: SettingEntity? = null,
    database: AppDatabase? = null
) {
    val context = LocalContext.current
    val store = remember { TeachingStore.getInstance(context) }
    val snapshot by store.snapshot.collectAsState()
    val isRefreshing by store.isRefreshing.collectAsState()
    val isSignedIn by store.isSignedIn.collectAsState()
    val downloadStates by TeachingDownloader.downloadStates.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedKind by remember { mutableStateOf(TeachingKind.ASSIGNMENT) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var isBatchImporting by remember { mutableStateOf(false) }
    var courseFilter by remember { mutableStateOf("") }

    // Collect courses and imported assignments
    val courses by remember(term, database) {
        if (term != null && database != null) {
            database.courseDao().getByTermId(term.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val importedIds by remember(term, database) {
        if (term != null && database != null) {
            database.assignmentDao().getImportedSourceIds(term.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val importedSet = remember(importedIds) { importedIds.toSet() }

    if (selectedItemId != null) {
        TeachingItemView(
            itemId = selectedItemId!!,
            term = term,
            database = database,
            onNavigateBack = { selectedItemId = null }
        )
        return
    }

    if (!isSignedIn) {
        TeachingSignInScreen(
            onNavigateBack = onNavigateBack,
            onSignInSuccess = {
                scope.launch { store.refresh() }
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("北大教学网") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { store.refresh() } },
                        enabled = !isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                    TextButton(onClick = { store.signOut() }) {
                        Text("退出登录")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = TeachingKind.entries.indexOf(selectedKind)) {
                TeachingKind.entries.forEach { kind ->
                    val title = when (kind) {
                        TeachingKind.ANNOUNCEMENT -> "通知"
                        TeachingKind.ASSIGNMENT -> "作业"
                        TeachingKind.MATERIAL -> "资料课件"
                    }
                    Tab(
                        selected = selectedKind == kind,
                        onClick = { selectedKind = kind },
                        text = { Text(title) }
                    )
                }
            }

            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (snapshot.courses.isNotEmpty()) {
                var filterExpanded by remember { mutableStateOf(false) }
                val selectedCourseTitle = snapshot.courses.firstOrNull { it.id == courseFilter }?.displayTitle ?: "全部课程"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { filterExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "课程筛选: $selectedCourseTitle",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "切换 ▾",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = filterExpanded,
                            onDismissRequest = { filterExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部课程") },
                                onClick = {
                                    courseFilter = ""
                                    filterExpanded = false
                                }
                            )
                            snapshot.courses.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.displayTitle) },
                                    onClick = {
                                        courseFilter = c.id
                                        filterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            val filteredItems = snapshot.items.filter { 
                it.kind == selectedKind && (courseFilter.isEmpty() || it.courseID == courseFilter)
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "暂无${when(selectedKind) {
                                TeachingKind.ANNOUNCEMENT -> "通知"
                                TeachingKind.ASSIGNMENT -> "作业"
                                TeachingKind.MATERIAL -> "资料"
                            }}数据",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "可点击右上角刷新按钮从教学网重新同步",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Card for Assignment Tab: One-Click Batch Import
                    if (selectedKind == TeachingKind.ASSIGNMENT && term != null && database != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "⚡ 一键导入全部作业",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    val currentImportedCount = filteredItems.count { importedSet.contains(it.id) }
                                    Text(
                                        "当前学期：${term.displayName} · 共 ${filteredItems.size} 项作业 (已导入 $currentImportedCount 项)\n系统将自动清洗课名并匹配本地课表课程。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Button(
                                        onClick = {
                                            if (!isBatchImporting) {
                                                isBatchImporting = true
                                                scope.launch {
                                                    try {
                                                        val count = TeachingImporter.importAll(
                                                            items = filteredItems,
                                                            term = term,
                                                            courses = courses,
                                                            database = database
                                                        )
                                                        snackbarHostState.showSnackbar("成功将 $count 项作业导入至「${term.displayName}」待办列表！")
                                                    } catch (e: Exception) {
                                                        snackbarHostState.showSnackbar("导入失败: ${e.localizedMessage}")
                                                    } finally {
                                                        isBatchImporting = false
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isBatchImporting,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isBatchImporting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("正在智能匹配导入中...")
                                        } else {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("一键导入全部作业到课表待办")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Item lists
                    items(filteredItems) { item ->
                        when (selectedKind) {
                            TeachingKind.ASSIGNMENT -> {
                                AssignmentCard(
                                    item = item,
                                    isImported = importedSet.contains(item.id),
                                    onOpenDetail = {
                                        store.markRead(item.id)
                                        selectedItemId = item.id
                                    },
                                    onQuickImport = {
                                        if (term != null && database != null) {
                                            scope.launch {
                                                val matched = TeachingImporter.findMatchingCourse(item, courses)
                                                TeachingImporter.apply(
                                                    item = item,
                                                    term = term,
                                                    course = matched,
                                                    database = database
                                                )
                                                val courseName = matched?.name?.let { " (关联: $it)" } ?: ""
                                                snackbarHostState.showSnackbar("已成功导入作业「${item.title}」$courseName")
                                            }
                                        }
                                    }
                                )
                            }
                            TeachingKind.MATERIAL -> {
                                MaterialCard(
                                    item = item,
                                    downloadStates = downloadStates,
                                    onOpenDetail = {
                                        store.markRead(item.id)
                                        selectedItemId = item.id
                                    },
                                    onDownloadAttachment = { attName, attUrl ->
                                        scope.launch {
                                            val res = TeachingDownloader.download(context, attUrl, attName)
                                            if (res.isSuccess) {
                                                snackbarHostState.showSnackbar("「$attName」下载成功！已存入系统下载目录。")
                                            } else {
                                                snackbarHostState.showSnackbar("下载失败: ${res.exceptionOrNull()?.message}")
                                            }
                                        }
                                    }
                                )
                            }
                            TeachingKind.ANNOUNCEMENT -> {
                                val isUnread = !snapshot.readKeys.contains(item.id) && !snapshot.readKeys.contains(item.itemReadKey)
                                AnnouncementCard(
                                    item = item,
                                    isUnread = isUnread,
                                    onOpenDetail = {
                                        store.markRead(item.id)
                                        store.markRead(item.itemReadKey)
                                        selectedItemId = item.id
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentCard(
    item: TeachingItem,
    isImported: Boolean,
    onOpenDetail: () -> Unit,
    onQuickImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenDetail() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.displayCourseTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isImported) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "已导入",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (item.dueDate != null) {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                Text(
                    text = "截止: ${format.format(item.dueDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (item.attachments.isNotEmpty()) {
                    Text(
                        "📎 附件 (${item.attachments.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onQuickImport,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (isImported) "重新导入" else "⚡ 一键导入", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(
    item: TeachingItem,
    downloadStates: Map<String, DownloadStatus>,
    onOpenDetail: () -> Unit,
    onDownloadAttachment: (name: String, url: String) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenDetail() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.displayCourseTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.publishedText != null) {
                Text(
                    text = item.publishedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Direct download list for attachments
            if (item.attachments.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "课件/资料附件 (${item.attachments.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        item.attachments.forEach { att ->
                            val localFile = TeachingDownloader.getDownloadedFile(context, att.name)
                            val state = downloadStates[att.url] ?: DownloadStatus.Idle

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        att.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1
                                    )
                                }

                                if (state is DownloadStatus.Downloading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else if (localFile != null || state is DownloadStatus.Success) {
                                    val file = localFile ?: (state as DownloadStatus.Success).file
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilledTonalButton(
                                            onClick = {
                                                if (!TeachingDownloader.openFile(context, file)) {
                                                    Toast.makeText(context, "无法打开此文件类型", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("打开", style = MaterialTheme.typography.labelSmall)
                                        }
                                        IconButton(
                                            onClick = { TeachingDownloader.shareFile(context, file) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "分享", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    FilledTonalButton(
                                        onClick = { onDownloadAttachment(att.name, att.url) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("直接下载", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    item: TeachingItem,
    isUnread: Boolean,
    onOpenDetail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenDetail() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(AppTheme.colors.accent, CircleShape)
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = item.displayCourseTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.publishedText != null) {
                Text(
                    text = item.publishedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
