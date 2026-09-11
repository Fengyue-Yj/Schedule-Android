package com.schedule.app.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.AssignmentEntity
import com.schedule.app.data.models.ExamEntity
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.ui.components.PageHeader
import com.schedule.app.ui.tasks.AssignmentDetailView
import com.schedule.app.ui.tasks.ExamDetailView
import com.schedule.app.ui.teaching.TeachingHubScreen
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.util.UpcomingEvent

@Composable
fun InsightsScreen(
    term: SettingEntity,
    database: AppDatabase
) {
    var showTeachingHub by remember { mutableStateOf(false) }

    if (showTeachingHub) {
        TeachingHubScreen(
            onNavigateBack = { showTeachingHub = false },
            term = term,
            database = database
        )
        return
    }

    val termId = term.id
    val assignments by database.assignmentDao().getByTermId(termId).collectAsState(initial = emptyList())
    val exams by database.examDao().getByTermId(termId).collectAsState(initial = emptyList())
    val courses by database.courseDao().getByTermId(termId).collectAsState(initial = emptyList())
    val allCourses by database.courseDao().getAll().collectAsState(initial = emptyList())
    val plans by database.flexiblePlanDao().getByTermId(termId).collectAsState(initial = emptyList())

    val context = androidx.compose.ui.platform.LocalContext.current
    val teachingStore = remember { com.schedule.app.teaching.TeachingStore.getInstance(context) }
    val teachingSnapshot by teachingStore.snapshot.collectAsState()

    val displayCourseCount = remember(courses, allCourses, teachingSnapshot.courses) {
        when {
            courses.isNotEmpty() -> courses.size
            allCourses.isNotEmpty() -> allCourses.size
            teachingSnapshot.courses.isNotEmpty() -> teachingSnapshot.courses.size
            else -> 0
        }
    }

    val events = remember(assignments, exams) {
        UpcomingEvent.collect(assignments, exams)
    }

    val completedAssignments = remember(assignments) {
        assignments.count { it.isCompleted }
    }

    val completedExams = remember(exams) {
        val now = System.currentTimeMillis()
        exams.count { it.date < now }
    }

    val nextStep = remember(plans) {
        plans.firstOrNull { it.status == "ACTIVE" && it.nextStep.isNotBlank() }?.nextStep
    }

    var selectedAssignment by remember { mutableStateOf<AssignmentEntity?>(null) }
    var selectedExam by remember { mutableStateOf<ExamEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.Spacing.page)
            .padding(top = 4.dp)
    ) {
        PageHeader(title = "Insights")

        Spacer(modifier = Modifier.height(16.dp))

        InsightsHeaderPanel(
            events = events,
            nextStep = nextStep,
            onSelect = { event ->
                if (event.isExam) {
                    selectedExam = exams.find { it.id == event.id }
                } else {
                    selectedAssignment = assignments.find { it.id == event.id }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TeachingNetworkCard(
            onNavigateToTeaching = { showTeachingHub = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        MetricsRow(
            coursesCount = displayCourseCount,
            completedAssignments = completedAssignments,
            totalAssignments = assignments.size,
            completedExams = completedExams,
            totalExams = exams.size
        )

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (selectedAssignment != null) {
        AssignmentDetailView(
            item = selectedAssignment!!,
            courses = courses,
            database = database,
            onDismiss = { selectedAssignment = null }
        )
    }

    if (selectedExam != null) {
        ExamDetailView(
            item = selectedExam!!,
            courses = courses,
            database = database,
            onDismiss = { selectedExam = null }
        )
    }
}
