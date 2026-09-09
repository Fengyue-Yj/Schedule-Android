package com.schedule.app.ui.calendar

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.ui.components.PageHeader
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    term: SettingEntity,
    database: AppDatabase
) {
    val termId = term.id
    
    val assignments by database.assignmentDao().getByTermId(termId).collectAsState(initial = emptyList())
    val exams by database.examDao().getByTermId(termId).collectAsState(initial = emptyList())
    
    val baseIndex = 24
    val pageCount = 49
    
    val pagerState = rememberPagerState(
        initialPage = baseIndex,
        pageCount = { pageCount }
    )
    
    val coroutineScope = rememberCoroutineScope()
    
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(Unit) {
        pagerState.scrollToPage(baseIndex)
    }
    
    val currentMonthMillis = remember(pagerState.currentPage) {
        val offset = pagerState.currentPage - baseIndex
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, offset)
        }
        cal.timeInMillis
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PageHeader(title = "Calendar", subtitle = null)
        
        MonthHeader(
            currentMonthMillis = currentMonthMillis,
            onPrevious = {
                val target = pagerState.currentPage - 1
                if (target >= 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(target, animationSpec = tween(300))
                    }
                }
            },
            onNext = {
                val target = pagerState.currentPage + 1
                if (target < pageCount) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(target, animationSpec = tween(300))
                    }
                }
            }
        )
        
        WeekdayRow()
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(44.dp * 6 + 8.dp * 5)
        ) { page ->
            val monthOffset = page - baseIndex
            val monthMillis = remember(monthOffset) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MONTH, monthOffset)
                }
                cal.timeInMillis
            }
            
            MonthGrid(
                currentMonth = monthMillis,
                selectedDate = selectedDate,
                assignments = assignments,
                exams = exams,
                onSelectDate = { date ->
                    selectedDate = date
                }
            )
        }
        
        DayAgendaView(
            date = selectedDate,
            assignments = assignments,
            exams = exams
        )
    }
}

@Composable
private fun MonthHeader(
    currentMonthMillis: Long,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthTitle = formatter.format(Date(currentMonthMillis))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Month",
                tint = AppTheme.colors.deepGreen
            )
        }
        
        Text(
            text = monthTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = AppTheme.colors.deepGreen
            )
        }
    }
}

@Composable
private fun WeekdayRow() {
    val titles = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        titles.forEach { title ->
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}
