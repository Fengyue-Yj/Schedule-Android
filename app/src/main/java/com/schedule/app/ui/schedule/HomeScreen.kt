package com.schedule.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.CourseWithMeetings
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.ui.components.HeaderActionButton
import com.schedule.app.ui.components.PageHeader
import com.schedule.app.ui.settings.SettingsScreen
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.util.CalendarManager

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    term: SettingEntity,
    database: AppDatabase,
    onTermChanged: (SettingEntity) -> Unit = {}
) {
    val coursesWithMeetings by database.courseDao().getCourseWithMeetings(term.id).collectAsState(initial = emptyList())

    val totalWeeks = maxOf(term.totalWeeks, 1)
    val initialWeek = CalendarManager.currentWeekIndex(
        termStartDate = term.termStartDate,
        totalWeeks = term.totalWeeks
    )

    val pagerState = rememberPagerState(
        initialPage = maxOf(0, minOf(initialWeek - 1, totalWeeks - 1)),
        pageCount = { totalWeeks }
    )
    val selectedWeek = pagerState.currentPage + 1

    var showNewCourseSheet by remember { mutableStateOf(false) }
    var selectedCourseForDetail by remember { mutableStateOf<CourseWithMeetings?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppTheme.spacing.page)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PageHeader(
                title = "Schedule",
                subtitle = "${term.displayName} · Week $selectedWeek of $totalWeeks"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeaderActionButton(
                        title = "Add Course",
                        icon = Icons.Default.Add,
                        onClick = { showNewCourseSheet = true }
                    )
                    HeaderActionButton(
                        title = "Settings",
                        icon = Icons.Default.Tune,
                        onClick = { showSettingsSheet = true }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val week = page + 1
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeekDateHeaderRow(week = week, setting = term)
                    WeekGridView(
                        week = week,
                        setting = term,
                        courses = coursesWithMeetings,
                        onCourseTap = { course ->
                            selectedCourseForDetail = course
                        }
                    )
                }
            }
        }
    }

    if (showNewCourseSheet) {
        NewCourseSheet(
            term = term,
            database = database,
            onDismiss = { showNewCourseSheet = false }
        )
    }

    selectedCourseForDetail?.let { course ->
        CourseDetailSheet(
            courseWithMeetings = course,
            database = database,
            onDismiss = { selectedCourseForDetail = null }
        )
    }

    if (showSettingsSheet) {
        SettingsScreen(
            term = term,
            database = database,
            onDismiss = { showSettingsSheet = false },
            onTermChanged = onTermChanged
        )
    }
}
