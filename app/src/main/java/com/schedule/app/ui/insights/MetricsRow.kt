package com.schedule.app.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedule.app.ui.components.cardBackground
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.ui.theme.sectionTitle

@Composable
fun MetricsRow(
    coursesCount: Int,
    completedAssignments: Int,
    totalAssignments: Int,
    completedExams: Int,
    totalExams: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard("Courses", coursesCount.toString(), modifier = Modifier.weight(1f))
        MetricCard("Assignments", "$completedAssignments/$totalAssignments", modifier = Modifier.weight(1f))
        MetricCard("Exams", "$completedExams/$totalExams", modifier = Modifier.weight(1f))
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.cardBackground(cornerRadius = AppTheme.Radius.card),
        shape = RoundedCornerShape(AppTheme.Radius.card),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = AppTheme.typography.sectionTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
