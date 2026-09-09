package com.schedule.app.ui.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.ui.theme.caption
import com.schedule.app.ui.theme.rowTitle
import com.schedule.app.ui.theme.sectionTitle
import com.schedule.app.util.UpcomingEvent

@Composable
fun UpcomingSummary(
    events: List<UpcomingEvent>,
    onSelect: (UpcomingEvent) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Coming Up",
            style = AppTheme.typography.sectionTitle,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        if (events.isEmpty()) {
            Text(
                text = "No upcoming deadlines. A little breathing room.",
                style = AppTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            events.take(2).forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(event) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (event.isExam) Icons.Outlined.Event else Icons.Default.Checklist,
                        contentDescription = null,
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = event.title,
                            style = AppTheme.typography.rowTitle,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = event.relativeTime(),
                            style = AppTheme.typography.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
