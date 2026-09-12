package com.schedule.app.ui.teaching

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
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
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.teaching.TeachingDownloader
import com.schedule.app.teaching.TeachingImporter
import com.schedule.app.teaching.TeachingItem
import com.schedule.app.teaching.TeachingKind
import com.schedule.app.teaching.TeachingParser
import com.schedule.app.teaching.TeachingStore
import com.schedule.app.ui.components.*
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.util.DateFormatUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingHubScreen(
    onNavigateBack: () -> Unit,
    term: SettingEntity? = null,
    database: AppDatabase? = null,
    initialKind: TeachingKind = TeachingKind.GRADE,
    initialCourseFilter: String = ""
) {
    val context = LocalContext.current
    val store = remember { TeachingStore.getInstance(context) }
    val snapshot by store.snapshot.collectAsState()
    val isRefreshing by store.isRefreshing.collectAsState()
    val isSignedIn by store.isSignedIn.collectAsState()
    val storeMessage by store.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedKind by remember { mutableStateOf(initialKind) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var courseFilter by remember { mutableStateOf(initialCourseFilter) }
    var showLoginScreen by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showCoursePickerSheet by remember { mutableStateOf(false) }
    var isBatchImporting by remember { mutableStateOf(false) }
    var isBatchDownloading by remember { mutableStateOf(false) }

    // Collect courses from Room database
    val courses by remember(term, database) {
        if (term != null && database != null) {
            database.courseDao().getByTermId(term.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    // Auto-match initial course filter if passed as local course ID
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

    LaunchedEffect(isSignedIn) {
        if (isSignedIn) {
            store.refreshIfNeeded()
        }
    }

    // Collect imported assignment IDs from Room database
    val importedIds by remember(term, database) {
        if (term != null && database != null) {
            database.assignmentDao().getImportedSourceIds(term.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val importedSet = remember(importedIds) { importedIds.toSet() }

    // Detail view navigation
    if (selectedItemId != null) {
        TeachingItemView(
            itemId = selectedItemId!!,
            term = term,
            database = database,
            onNavigateBack = { selectedItemId = null }
        )
        return
    }

    // Login screen sheet
    if (showLoginScreen || !isSignedIn) {
        TeachingSignInScreen(
            onNavigateBack = {
                if (!isSignedIn) {
                    onNavigateBack()
                } else {
                    showLoginScreen = false
                }
            },
            onSignInSuccess = {
                showLoginScreen = false
                scope.launch { store.refresh() }
            }
        )
        return
    }

    val displayedKinds = listOf(TeachingKind.GRADE, TeachingKind.ASSIGNMENT, TeachingKind.MATERIAL)

    val filteredItems = snapshot.items.filter { 
        it.kind == selectedKind && (courseFilter.isEmpty() || it.courseID == courseFilter)
    }.let { list ->
        TeachingItem.newestFirst(list)
    }

    val selectedCourseTitle = snapshot.courses.firstOrNull { it.id == courseFilter }?.displayTitle ?: "全部课程"

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
                        text = "北大教学网",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Right action: iOS "ellipsis.circle" more menu
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = AppTheme.colors.accent.copy(alpha = 0.1f),
                            modifier = Modifier
                                .size(32.dp)
                                .iosPressable { showOptionsMenu = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "更多选项",
                                    tint = AppTheme.colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isRefreshing) "正在同步中..." else "同步最新数据") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = if (isRefreshing) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else AppTheme.colors.accent
                                    )
                                },
                                enabled = !isRefreshing,
                                onClick = {
                                    showOptionsMenu = false
                                    scope.launch {
                                        try {
                                            store.refresh()
                                            Toast.makeText(context, "教学网数据同步完成", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            if (e is com.schedule.app.teaching.TeachingError.LoginRequired || !store.isSignedIn.value) {
                                                Toast.makeText(context, "登录会话已过期，请重新登录教学网", Toast.LENGTH_LONG).show()
                                                showLoginScreen = true
                                            } else {
                                                Toast.makeText(context, "同步失败: ${e.localizedMessage ?: "网络错误"}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )

                            if (selectedKind == TeachingKind.ASSIGNMENT && term != null && database != null && filteredItems.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(if (isBatchImporting) "正在导入..." else "一键导入全部作业") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            tint = AppTheme.colors.accent
                                        )
                                    },
                                    enabled = !isBatchImporting,
                                    onClick = {
                                        showOptionsMenu = false
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
                                )
                            }

                            if (selectedKind == TeachingKind.MATERIAL && filteredItems.any { it.attachments.isNotEmpty() }) {
                                val allAttachments = filteredItems.flatMap { item -> item.attachments.map { att -> Pair(att, item) } }
                                DropdownMenuItem(
                                    text = { Text(if (isBatchDownloading) "正在下载..." else "一键下载全部资料 (${allAttachments.size})") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Download,
                                            contentDescription = null,
                                            tint = AppTheme.colors.accent
                                        )
                                    },
                                    enabled = !isBatchDownloading,
                                    onClick = {
                                        showOptionsMenu = false
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
                                )
                            }

                            if (selectedKind == TeachingKind.GRADE && filteredItems.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("全部标为已读") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DoneAll,
                                            contentDescription = null,
                                            tint = AppTheme.colors.accent
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        store.markAllAnnouncementsRead()
                                    }
                                )
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = AppTheme.colors.border.copy(alpha = 0.35f)
                            )

                            DropdownMenuItem(
                                text = { Text("重新登录") },
                                onClick = {
                                    showOptionsMenu = false
                                    showLoginScreen = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("退出登录", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOptionsMenu = false
                                    store.signOut()
                                }
                            )
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = AppTheme.colors.border.copy(alpha = 0.35f)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. iOS Status & Course Filter Section
            item {
                IosFormSection {
                    // Row 1: Connection status & refresh state
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isSignedIn) "已连接教学网" else "教学网未连接 (已离线)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 17.sp
                                ),
                                color = if (isSignedIn) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                            )
                            val fetchedAt = snapshot.fetchedAt
                            if (fetchedAt > 0) {
                                Text(
                                    text = if (isSignedIn) "更新于 ${DateFormatUtil.formatDateTime(fetchedAt)}" else "上次同步: ${DateFormatUtil.formatDateTime(fetchedAt)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AppTheme.colors.accent
                            )
                        } else if (!isSignedIn) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppTheme.colors.accent,
                                modifier = Modifier.iosPressable { showLoginScreen = true }
                            ) {
                                Text(
                                    text = "重新登录",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Progress or status message
                    if (!storeMessage.isNullOrBlank()) {
                        IosFormDivider()
                        val isError = storeMessage!!.contains("失败") || storeMessage!!.contains("过期") || storeMessage!!.contains("异常")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = storeMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = if (isError) MaterialTheme.colorScheme.error else AppTheme.colors.accent,
                                modifier = Modifier.weight(1f)
                            )
                            if (isError && !isSignedIn) {
                                Text(
                                    text = "去登录",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                    color = AppTheme.colors.accent,
                                    modifier = Modifier.iosPressable { showLoginScreen = true }
                                )
                            }
                        }
                    }

                    // Row 2: Course Filter (iOS Form Row)
                    if (isSignedIn && snapshot.courses.isNotEmpty()) {
                        IosFormDivider()
                        IosFormRow(
                            label = "课程",
                            value = selectedCourseTitle,
                            showChevron = true,
                            onClick = { showCoursePickerSheet = true }
                        )
                    }
                }
            }

            // 2. Category Picker (Segmented Control)
            item {
                AppSegmentedPicker(
                    label = "教学网分类",
                    selection = selectedKind,
                    options = displayedKinds,
                    title = { it.title },
                    onSelectionChange = { selectedKind = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            // 3. Items or Empty State
            if (filteredItems.isEmpty()) {
                item {
                    AppEmptyState(
                        title = if (isSignedIn) "暂无${selectedKind.title}" else "所有课程，汇于一处",
                        message = if (isSignedIn) "点击右上角更多选项同步最新数据，或切换课程筛选。" else "登录以查看成绩、导入作业和下载课件资料。",
                        icon = Icons.Default.AccountBalance,
                        modifier = Modifier.padding(top = 40.dp)
                    )
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    TeachingItemCard(
                        item = item,
                        isImported = importedSet.contains(item.id),
                        isUnread = !snapshot.readKeys.contains(item.id) && !snapshot.readKeys.contains(item.itemReadKey),
                        onOpen = {
                            store.markRead(item.id)
                            store.markRead(item.itemReadKey)
                            selectedItemId = item.id
                        }
                    )
                }
            }
        }
    }

    // iOS Course Picker Modal Bottom Sheet
    if (showCoursePickerSheet) {
        IosModalBottomSheet(
            onDismissRequest = { showCoursePickerSheet = false }
        ) {
            IosSheetHeader(
                title = "选择课程",
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
                        // "全部课程" option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .iosPressable {
                                    courseFilter = ""
                                    showCoursePickerSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "全部课程",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (courseFilter.isEmpty()) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (courseFilter.isEmpty()) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurface
                            )
                            if (courseFilter.isEmpty()) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppTheme.colors.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Courses list
                        snapshot.courses.forEach { course ->
                            IosFormDivider()
                            val isSelected = courseFilter == course.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .iosPressable {
                                        courseFilter = course.id
                                        showCoursePickerSheet = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = course.displayTitle,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
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

/**
 * Unified authentic iOS Item Card for Teaching Hub
 */
@Composable
private fun TeachingItemCard(
    item: TeachingItem,
    isImported: Boolean,
    isUnread: Boolean,
    onOpen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardBackground(cornerRadius = 14.dp)
            .iosPressable(onClick = onOpen)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Unread indicator dot (7dp circle)
            if ((item.kind == TeachingKind.GRADE || item.kind == TeachingKind.ANNOUNCEMENT) && isUnread) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(AppTheme.colors.accent, CircleShape)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Item title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Course title
                Text(
                    text = item.displayCourseTitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Meta row based on kind
                when (item.kind) {
                    TeachingKind.GRADE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!item.gradeCategory.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = item.gradeCategory,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!item.feedback.isNullOrBlank()) {
                                Text(
                                    text = "💬 教师评语",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = AppTheme.colors.accent
                                )
                            }
                        }
                    }
                    TeachingKind.ASSIGNMENT -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (item.dueDate != null) {
                                Text(
                                    text = DateFormatUtil.formatDateTime(item.dueDate),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = AppTheme.colors.accent
                                )
                            } else {
                                Text(
                                    text = "未提供截止时间",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            if (isImported) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AppTheme.colors.accent.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "已导入",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = AppTheme.colors.accent,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (item.attachments.isNotEmpty()) {
                                Text(
                                    text = "📎 ${item.attachments.size} 个文件",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    TeachingKind.MATERIAL -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val pubTime = item.publishedAt ?: item.publishedText?.let { TeachingParser.parseDate(it)?.time }
                            if (pubTime != null) {
                                Text(
                                    text = DateFormatUtil.formatDateTime(pubTime),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.attachments.isNotEmpty()) {
                                Text(
                                    text = "📎 ${item.attachments.size} 个文件",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    TeachingKind.ANNOUNCEMENT -> {
                        val pubTime = item.publishedAt ?: item.publishedText?.let { TeachingParser.parseDate(it)?.time }
                        if (pubTime != null) {
                            Text(
                                text = DateFormatUtil.formatDateTime(pubTime),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Right side: Score badge for GRADE
            if (item.kind == TeachingKind.GRADE) {
                val hasScore = !item.score.isNullOrBlank()
                val isPending = item.gradeStatus == "待评分" || item.score == "待评分"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isPending -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        hasScore -> AppTheme.colors.accent.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isPending) {
                            Text(
                                text = "待评分",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        } else if (hasScore) {
                            Text(
                                text = item.score!!,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.accent
                            )
                            if (!item.pointsPossible.isNullOrBlank()) {
                                Text(
                                    text = "/${item.pointsPossible}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "未出分",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Subtle iOS chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
