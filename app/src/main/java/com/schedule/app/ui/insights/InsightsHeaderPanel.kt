package com.schedule.app.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.ui.components.cardBackground
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.util.UpcomingEvent

@Composable
fun InsightsHeaderPanel(
    events: List<UpcomingEvent> = emptyList(),
    nextStep: String? = null,
    onSelect: (UpcomingEvent) -> Unit = {}
) {
    var showingCompanion by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showingCompanion) {
            CompanionCard(
                events = events,
                nextStep = nextStep,
                onSelect = onSelect,
                onHide = { showingCompanion = false }
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .cardBackground(cornerRadius = AppTheme.Radius.card),
                shape = RoundedCornerShape(AppTheme.Radius.card),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    UpcomingSummary(events = events, onSelect = onSelect)
                }
            }
            TextButton(
                onClick = { showingCompanion = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Companion", color = AppTheme.colors.accent)
            }
        }
    }
}
