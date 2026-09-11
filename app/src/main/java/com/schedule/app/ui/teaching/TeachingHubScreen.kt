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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.schedule.app.util.DateFormatUtil
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
    database: AppDatabase? = null,
    initialKind: TeachingKind = TeachingKind.ASSIGNMENT,
    initialCourseFilter: String = ""
) {
    val context = LocalContext.current
    val store = remember { TeachingStore.getInstance(context) }
    val snapshot by store.snapshot.collectAsState()
    val isRefreshing by store.isRefreshing.collectAsState()
    val isSignedIn by store.isSignedIn.collectAsState()
    val downloadStates by TeachingDownloader.downloadStates.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedKind by remember { mutableStateOf(initialKind) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var isBatchImporting by remember { mutableStateOf(false) }
    var isBatchDownloading by remember { mutableStateOf(false) }
    var courseFilter by remember { mutableStateOf(initialCourseFilter) }

    // Collect courses and imported assignments
    val courses by remember(term, database) {
        if (term != null && database != null) {
            database.courseDao().getByTermId(term.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val matchingTeachingCourseId = remember(snapshot.courses, snapshot.courseLinks, initialCourseFilter, courses) {
        if (initialCourseFilter.isEmpty()) ""
        else {
            if (snapshot.courses.any { it.id == initialCourseFilter }) {
                initialCourseFilter
            } else {
                val localCourse = courses.firstOrNull { it.id == initialCourseFilter }
                val linkedTeachingId = snapshot.courseLinks.entries.firstOrNull { it.value == initialCourseFilter }?.key
                if (linkedTeachingId != null) {
                    linkedTeachingId
                } else if (localCourse != null) {
                    snapshot.courses.firstOrNull { 
                        it.displayTitle.contains(localCourse.name) || localCourse.name.contains(it.displayTitle)
                    }?.id ?: ""
                } else ""
            }
        }
    }

    LaunchedEffect(matchingTeachingCourseId) {
        if (matchingTeachingCourseId.isNotEmpty()) {
            courseFilter = matchingTeachingCourseId
        }
    }

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
                title = { Text("北大教学网") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { scope.launch { store.refresh() } },
                        enabled = !isRefreshing,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isRefreshing) "同步中" else "同步", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { store.signOut() }) {
                        Text("退出")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val displayedKinds = listOf(TeachingKind.GRADE, TeachingKind.ASSIGNMENT, TeachingKind.MATERIAL)
            TabRow(selectedTabIndex = displayedKinds.indexOf(selectedKind).coerceAtLeast(0)) {
                displayedKinds.forEach { kind ->
                    val title = when (kind) {
                        TeachingKind.GRADE -> "成绩"
                        TeachingKind.ASSIGNMENT -> "作业"
                        TeachingKind.MATERIAL -> "资料课件"
                        TeachingKind.ANNOUNCEMENT -> "通知"
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
            }.let { list ->
                if (selectedKind == TeachingKind.ANNOUNCEMENT) {
                    TeachingItem.newestFirst(list)
                } else {
                    list
                }
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "暂无${when(selectedKind) {
                                TeachingKind.GRADE -> "成绩"
                                TeachingKind.ASSIGNMENT -> "作业"
                                TeachingKind.MATERIAL -> "资料课件"
                                TeachingKind.ANNOUNCEMENT -> "通知"
                            }}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { scope.launch { store.refresh() } },
                            enabled = !isRefreshing
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isRefreshing) "正在同步..." else "从教学网同步最新数据")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Compact Action Bar for Assignment Tab
                    if (selectedKind == TeachingKind.ASSIGNMENT && term != null && database != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                                    val msg = "已成功导入 $count 项作业至待办列表"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    snackbarHostState.showSnackbar(msg)
                                                } catch (e: Exception) {
                                                    val msg = "导入失败: ${e.localizedMessage}"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    snackbarHostState.showSnackbar(msg)
                                                } finally {
                                                    isBatchImporting = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isBatchImporting,
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isBatchImporting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("导入中...", style = MaterialTheme.typography.labelMedium)
                                    } else {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("一键导入全部作业", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { scope.launch { store.refresh() } },
                                    enabled = !isRefreshing,
                                    modifier = Modifier.height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isRefreshing) "同步中" else "同步作业", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Compact Action Bar for Material Tab
                    if (selectedKind == TeachingKind.MATERIAL && filteredItems.any { it.attachments.isNotEmpty() }) {
                        val allAttachments = filteredItems.flatMap { item -> item.attachments.map { att -> Pair(att, item) } }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (!isBatchDownloading) {
                                            isBatchDownloading = true
                                            scope.launch {
                                                try {
                                                    var succ = 0
                                                    var fail = 0
                                                    for ((att, sourceItem) in allAttachments) {
                                                        if (!TeachingDownloader.isDownloaded(context, att.name)) {
                                                            val res = TeachingDownloader.download(
                                                                context = context,
                                                                url = att.url,
                                                                suggestedFileName = att.name,
                                                                referer = sourceItem.sourceURL
                                                            )
                                                            if (res.isSuccess) succ++ else fail++
                                                        }
                                                    }
                                                    val msg = "批量下载完成: 成功 $succ 个${if (fail > 0) "，失败 $fail 个" else ""}"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    snackbarHostState.showSnackbar(msg)
                                                } catch (e: Exception) {
                                                    val msg = "批量下载出错: ${e.localizedMessage}"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    snackbarHostState.showSnackbar(msg)
                                                } finally {
                                                    isBatchDownloading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isBatchDownloading,
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isBatchDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("下载中...", style = MaterialTheme.typography.labelMedium)
                                    } else {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("一键下载全部资料 (${allAttachments.size})", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { scope.launch { store.refresh() } },
                                    enabled = !isRefreshing,
                                    modifier = Modifier.height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isRefreshing) "同步中" else "同步资料", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Compact Action Bar for Announcement Tab
                    if (selectedKind == TeachingKind.ANNOUNCEMENT && filteredItems.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                            ) {
                                OutlinedButton(
                                    onClick = { store.markAllAnnouncementsRead() },
                                    modifier = Modifier.height(36.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("全部已读", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = { scope.launch { store.refresh() } },
                                    enabled = !isRefreshing,
                                    modifier = Modifier.height(36.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (isRefreshing) "同步中" else "同步通知", style = MaterialTheme.typography.labelSmall)
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
                                                val created = TeachingImporter.apply(
                                                    item = item,
                                                    term = term,
                                                    course = matched,
                                                    database = database
                                                )
                                                if (created != null) {
                                                    val courseName = matched?.name?.let { " (关联: $it)" } ?: ""
                                                    snackbarHostState.showSnackbar("已成功导入作业「${item.title}」$courseName")
                                                }
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
                                            val res = TeachingDownloader.download(
                                                context = context,
                                                url = attUrl,
                                                suggestedFileName = attName,
                                                referer = item.sourceURL
                                            )
                                            if (res.isSuccess) {
                                                val msg = "「$attName」下载成功！已存入系统下载目录。"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                snackbarHostState.showSnackbar(msg)
                                            } else {
                                                val msg = "下载失败: ${res.exceptionOrNull()?.message}"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    }
                                )
                            }
                            TeachingKind.GRADE -> {
                                GradeCard(
                                    item = item,
                                    onOpenDetail = {
                                        selectedItemId = item.id
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
                Text(
                    text = "截止: ${DateFormatUtil.formatDateTime(item.dueDate)}",
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
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
            val pubTime = item.publishedAt ?: item.publishedText?.let { TeachingParser.parseDate(it)?.time }
            val pubDisplay = when {
                pubTime != null -> DateFormatUtil.formatDateTime(pubTime)
                !item.publishedText.isNullOrBlank() -> item.publishedText.replace("发布者:", "").trim()
                else -> null
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.displayCourseTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (pubDisplay != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pubDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (item.body.isNotBlank()) {
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.attachments.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${item.attachments.size} 个附件",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeCard(
    item: TeachingItem,
    onOpenDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.displayCourseTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!item.gradeCategory.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = item.gradeCategory,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Score Badge
                val hasScore = !item.score.isNullOrBlank()
                val isPending = item.gradeStatus == "待评分" || item.score == "待评分"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isPending -> MaterialTheme.colorScheme.tertiaryContainer
                        hasScore -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isPending) {
                            Text(
                                text = "待评分",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        } else if (hasScore) {
                            Text(
                                text = item.score!!,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!item.pointsPossible.isNullOrBlank()) {
                                Text(
                                    text = " / ${item.pointsPossible}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "未出分",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Feedback snippet if present
            if (!item.feedback.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💬 教师评语: ${item.feedback}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Footer: Date and details hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val pubTime = item.publishedAt ?: item.publishedText?.let { TeachingParser.parseDate(it)?.time }
                val dateDisplay = when {
                    pubTime != null -> DateFormatUtil.formatDateTime(pubTime)
                    !item.publishedText.isNullOrBlank() -> item.publishedText
                    else -> null
                }
                if (dateDisplay != null) {
                    Text(
                        text = "活动时间: $dateDisplay",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Text(
                    text = "查看详情 ▾",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
