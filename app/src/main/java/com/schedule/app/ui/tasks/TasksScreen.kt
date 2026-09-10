package com.schedule.app.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.*
import com.schedule.app.teaching.TeachingKind
import com.schedule.app.ui.components.*
import com.schedule.app.ui.teaching.TeachingHubScreen
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.ui.theme.caption
import com.schedule.app.ui.theme.rowTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class TaskSection(val title: String) {
    PLANS("Plans"), ASSIGNMENTS("Assignments"), EXAMS("Exams")
}

enum class AssignmentFilter(val title: String) {
    ALL("All"), PENDING("Pending"), COMPLETED("Completed")
}

enum class ExamFilter(val title: String) {
    ALL("All"), UPCOMING("Upcoming"), PAST("Past")
}

enum class PlanListFilter(val title: String) {
    ALL("All"), ACTIVE("Active"), PAUSED("Paused"), COMPLETED("Completed")
}

enum class TaskSheet {
    NEW_PLAN, NEW_ASSIGNMENT, NEW_EXAM
}

sealed class TaskDetailSheet {
    data class Plan(val plan: FlexiblePlanEntity) : TaskDetailSheet()
    data class Assignment(val assignment: AssignmentEntity) : TaskDetailSheet()
    data class Exam(val exam: ExamEntity) : TaskDetailSheet()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    term: SettingEntity,
    database: AppDatabase
) {
    val coroutineScope = rememberCoroutineScope()
    var showTeachingHub by remember { mutableStateOf(false) }

    if (showTeachingHub) {
        TeachingHubScreen(
            onNavigateBack = { showTeachingHub = false },
            term = term,
            database = database,
            initialKind = TeachingKind.ASSIGNMENT
        )
        return
    }
    
    var selection by remember { mutableStateOf(TaskSection.ASSIGNMENTS) }
    var activeSheet by remember { mutableStateOf<TaskSheet?>(null) }
    var detailSheet by remember { mutableStateOf<TaskDetailSheet?>(null) }
    
    var planFilter by remember { mutableStateOf(PlanListFilter.ALL) }
    var assignmentFilter by remember { mutableStateOf(AssignmentFilter.ALL) }
    var examFilter by remember { mutableStateOf(ExamFilter.ALL) }

    val assignments by database.assignmentDao().getByTermId(term.id).collectAsState(initial = emptyList())
    val exams by database.examDao().getByTermId(term.id).collectAsState(initial = emptyList())
    val courses by database.courseDao().getByTermId(term.id).collectAsState(initial = emptyList())

    val sortedAssignments = remember(assignments, assignmentFilter) {
        assignments.filter {
            when (assignmentFilter) {
                AssignmentFilter.ALL -> true
                AssignmentFilter.PENDING -> !it.isCompleted
                AssignmentFilter.COMPLETED -> it.isCompleted
            }
        }.sortedWith { a, b ->
            if (a.isCompleted != b.isCompleted) {
                if (b.isCompleted) -1 else 1
            } else if (a.isCompleted) {
                b.dueDate.compareTo(a.dueDate)
            } else {
                a.dueDate.compareTo(b.dueDate)
            }
        }
    }

    val sortedExams = remember(exams, examFilter) {
        val now = System.currentTimeMillis()
        exams.filter {
            when (examFilter) {
                ExamFilter.ALL -> true
                ExamFilter.UPCOMING -> it.date >= now
                ExamFilter.PAST -> it.date < now
            }
        }.sortedWith { a, b ->
            if (examFilter == ExamFilter.PAST) {
                b.date.compareTo(a.date)
            } else {
                a.date.compareTo(b.date)
            }
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .background(AppTheme.colors.background)
                    .padding(horizontal = AppTheme.Spacing.page)
                    .padding(top = 4.dp)
            ) {
                PageHeader(
                    title = "Tasks",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            when (selection) {
                                TaskSection.PLANS -> TaskFilterMenu(
                                    label = "Filter Plans",
                                    selection = planFilter,
                                    options = PlanListFilter.entries.toList(),
                                    optionTitle = { it.title },
                                    isFiltered = planFilter != PlanListFilter.ALL,
                                    onSelectionChange = { planFilter = it }
                                )
                                TaskSection.ASSIGNMENTS -> {
                                    TaskFilterMenu(
                                        label = "Filter Assignments",
                                        selection = assignmentFilter,
                                        options = AssignmentFilter.entries.toList(),
                                        optionTitle = { it.title },
                                        isFiltered = assignmentFilter != AssignmentFilter.ALL,
                                        onSelectionChange = { assignmentFilter = it }
                                    )
                                    HeaderActionButton(
                                        title = "教学网作业",
                                        icon = Icons.Default.CloudDownload,
                                        onClick = { showTeachingHub = true }
                                    )
                                }
                                TaskSection.EXAMS -> TaskFilterMenu(
                                    label = "Filter Exams",
                                    selection = examFilter,
                                    options = ExamFilter.entries.toList(),
                                    optionTitle = { it.title },
                                    isFiltered = examFilter != ExamFilter.ALL,
                                    onSelectionChange = { examFilter = it }
                                )
                            }
                            HeaderActionButton(
                                title = "Add Task",
                                systemImage = "plus",
                                onClick = {
                                    activeSheet = when (selection) {
                                        TaskSection.PLANS -> TaskSheet.NEW_PLAN
                                        TaskSection.ASSIGNMENTS -> TaskSheet.NEW_ASSIGNMENT
                                        TaskSection.EXAMS -> TaskSheet.NEW_EXAM
                                    }
                                }
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppSegmentedPicker(
                    label = "Section",
                    selection = selection,
                    options = TaskSection.entries.toList(),
                    title = { it.title },
                    onSelectionChange = { selection = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppTheme.Spacing.page)
        ) {
            when (selection) {
                TaskSection.PLANS -> {
                    PlansView(
                        term = term,
                        database = database,
                        filter = planFilter,
                        onOpen = { detailSheet = TaskDetailSheet.Plan(it) }
                    )
                }
                TaskSection.ASSIGNMENTS -> {
                    if (sortedAssignments.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AppEmptyState(
                                title = if (assignmentFilter == AssignmentFilter.ALL) "暂无作业" else "无${assignmentFilter.title}作业",
                                message = "可点击右上角「+」手动添加作业，或直接从北大教学网一键同步导入。",
                                systemImage = "checklist"
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showTeachingHub = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("⚡ 从北大教学网一键导入作业", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AppTheme.Spacing.row),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp)
                        ) {
                            item {
                                Surface(
                                    onClick = { showTeachingHub = true },
                                    shape = RoundedCornerShape(14.dp),
                                    color = AppTheme.colors.accent.copy(alpha = 0.08f),
                                    border = BorderStroke(
                                        0.8.dp,
                                        AppTheme.colors.accent.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            tint = AppTheme.colors.accent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "北大教学网作业同步",
                                                style = AppTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                                                color = AppTheme.colors.accent
                                            )
                                            Text(
                                                text = "点击一键拉取教学网最新作业与截止时间",
                                                style = AppTheme.typography.caption,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = AppTheme.colors.accent.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            items(sortedAssignments) { item ->
                                val course = courses.find { it.id == item.courseId }
                                AssignmentRow(
                                    item = item,
                                    courseName = course?.name,
                                    isDueSoon = isAssignmentDueSoon(item),
                                    onToggleComplete = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            database.assignmentDao().update(item.copy(isCompleted = !item.isCompleted))
                                        }
                                    },
                                    onOpen = { detailSheet = TaskDetailSheet.Assignment(item) },
                                    onDelete = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            database.assignmentDao().delete(item)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                TaskSection.EXAMS -> {
                    if (sortedExams.isEmpty()) {
                        AppEmptyState(
                            title = if (examFilter == ExamFilter.ALL) "No exams yet." else "No ${examFilter.title.lowercase()} exams.",
                            message = "Tap + to add an exam.",
                            systemImage = "calendar"
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AppTheme.Spacing.row),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp)
                        ) {
                            items(sortedExams) { item ->
                                ExamRow(
                                    item = item,
                                    daysUntil = daysUntilExam(item),
                                    onOpen = { detailSheet = TaskDetailSheet.Exam(item) },
                                    onDelete = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            database.examDao().delete(item)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeSheet != null) {
        when (activeSheet) {
            TaskSheet.NEW_PLAN -> {
                PlanEditor(
                    database = database,
                    termId = term.id,
                    onDismiss = { activeSheet = null },
                    onSave = { planFilter = PlanListFilter.ALL }
                )
            }
            TaskSheet.NEW_ASSIGNMENT -> {
                AssignmentEditor(
                    termId = term.id,
                    courses = courses,
                    database = database,
                    onDismiss = { activeSheet = null },
                    onSave = { assignmentFilter = AssignmentFilter.ALL }
                )
            }
            TaskSheet.NEW_EXAM -> {
                ExamEditor(
                    termId = term.id,
                    courses = courses,
                    database = database,
                    onDismiss = { activeSheet = null },
                    onSave = { examFilter = ExamFilter.ALL }
                )
            }
            null -> {}
        }
    }

    if (detailSheet != null) {
        when (val sheet = detailSheet) {
            is TaskDetailSheet.Plan -> {
                PlanDetailView(
                    plan = sheet.plan,
                    database = database,
                    onDismiss = { detailSheet = null }
                )
            }
            is TaskDetailSheet.Assignment -> {
                AssignmentDetailView(
                    item = sheet.assignment,
                    courses = courses,
                    database = database,
                    onDismiss = { detailSheet = null }
                )
            }
            is TaskDetailSheet.Exam -> {
                ExamDetailView(
                    item = sheet.exam,
                    courses = courses,
                    database = database,
                    onDismiss = { detailSheet = null }
                )
            }
            null -> {}
        }
    }
}

@Composable
private fun AssignmentRow(
    item: AssignmentEntity,
    courseName: String?,
    isDueSoon: Boolean,
    onToggleComplete: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    TaskRowCard(
        title = item.content,
        onOpen = onOpen,
        onDelete = onDelete,
        leading = {
            CompletionButton(
                isCompleted = item.isCompleted,
                title = item.content,
                onClick = onToggleComplete
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.content,
                        style = AppTheme.typography.rowTitle,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground
                    )
                    if (isDueSoon && !item.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Due Soon",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = dateFormat.format(item.dueDate),
                        style = AppTheme.typography.caption,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    if (courseName != null) {
                        Text(
                            text = courseName,
                            style = AppTheme.typography.caption,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ExamRow(
    item: ExamEntity,
    daysUntil: Int?,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    TaskRowCard(
        title = item.subject,
        onOpen = onOpen,
        onDelete = onDelete,
        leading = {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = AppTheme.colors.accent,
                modifier = Modifier.size(24.dp)
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.subject,
                        style = AppTheme.typography.rowTitle
                    )
                    if (daysUntil != null && daysUntil >= 0) {
                        Surface(
                            color = AppTheme.colors.accent.copy(alpha = 0.14f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "D-$daysUntil",
                                style = AppTheme.typography.caption,
                                color = AppTheme.colors.accent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (item.detail.isNotEmpty()) {
                    Text(
                        text = item.detail,
                        style = AppTheme.typography.caption,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = dateFormat.format(item.date),
                    style = AppTheme.typography.caption,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    )
}

private fun isAssignmentDueSoon(item: AssignmentEntity): Boolean {
    val today = LocalDate.now()
    val due = Instant.ofEpochMilli(item.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, due)
    return days in 0..3
}

private fun daysUntilExam(item: ExamEntity): Int? {
    val today = LocalDate.now()
    val exam = Instant.ofEpochMilli(item.date).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, exam)
    return if (days in 0..7) days.toInt() else null
}
