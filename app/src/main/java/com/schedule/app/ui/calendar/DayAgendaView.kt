package com.schedule.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.models.AssignmentEntity
import com.schedule.app.data.models.ExamEntity
import com.schedule.app.ui.components.cardBackground
import com.schedule.app.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DayAgendaView(
    date: Long?,
    assignments: List<AssignmentEntity>,
    exams: List<ExamEntity>
) {
    if (date == null) return
    
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    val title = formatter.format(Date(date))
    
    val dayAssignments = assignments.filter { isSameDay(it.dueDate, date) }
    val dayExams = exams.filter { isSameDay(it.date, date) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .cardBackground(cornerRadius = AppTheme.Radius.card),
        shape = RoundedCornerShape(AppTheme.Radius.card),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (dayAssignments.isEmpty() && dayExams.isEmpty()) {
                Text(
                    text = "No items scheduled for this day.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (dayAssignments.isNotEmpty()) {
                    Column {
                        dayAssignments.forEachIndexed { index, item ->
                            AgendaRow(
                                title = item.content,
                                subtitle = item.submitMethod.ifEmpty { "Assignment" },
                                dotColor = Color(0xFF007AFF)
                            )
                            if (index < dayAssignments.size - 1 || dayExams.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
                
                if (dayExams.isNotEmpty()) {
                    Column {
                        dayExams.forEachIndexed { index, item ->
                            AgendaRow(
                                title = item.subject,
                                subtitle = item.detail.ifEmpty { "Exam" },
                                dotColor = Color(0xFFFF2D55)
                            )
                            if (index < dayExams.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
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
private fun AgendaRow(
    title: String,
    subtitle: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
