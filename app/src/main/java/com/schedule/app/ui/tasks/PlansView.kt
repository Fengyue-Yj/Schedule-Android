package com.schedule.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.FlexiblePlanEntity
import com.schedule.app.data.models.PlanStatus
import com.schedule.app.data.models.SettingEntity
import com.schedule.app.ui.components.AppEmptyState
import com.schedule.app.ui.components.CompletionButton
import com.schedule.app.ui.components.TaskRowCard
import com.schedule.app.ui.theme.AppTheme
import com.schedule.app.ui.theme.caption
import com.schedule.app.ui.theme.rowTitle
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PlansView(
    term: SettingEntity,
    database: AppDatabase,
    filter: PlanListFilter,
    onOpen: (FlexiblePlanEntity) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val allPlans by database.flexiblePlanDao().getByTermId(term.id).collectAsState(initial = emptyList())
    
    val visiblePlans = remember(allPlans, filter) {
        allPlans.filter {
            filter == PlanListFilter.ALL || it.planStatus.name == filter.name
        }.sortedWith { a, b ->
            if (a.planStatus != b.planStatus) {
                a.planStatus.ordinal.compareTo(b.planStatus.ordinal)
            } else if (a.planStatus == PlanStatus.COMPLETED) {
                (b.completedAt ?: b.createdAt).compareTo(a.completedAt ?: a.createdAt)
            } else {
                b.createdAt.compareTo(a.createdAt)
            }
        }
    }

    if (visiblePlans.isEmpty()) {
        val title = when (filter) {
            PlanListFilter.ALL -> "No plans yet."
            PlanListFilter.ACTIVE -> "No active plans."
            PlanListFilter.PAUSED -> "No paused plans."
            PlanListFilter.COMPLETED -> "No completed plans."
        }
        val message = when (filter) {
            PlanListFilter.ALL, PlanListFilter.ACTIVE -> "Tap + to add a plan. Just a title is enough."
            PlanListFilter.PAUSED -> "Paused plans will appear here."
            PlanListFilter.COMPLETED -> "Completed plans will appear here."
        }
        AppEmptyState(
            title = title,
            message = message,
            systemImage = "leaf"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(AppTheme.Spacing.row),
            contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp)
        ) {
            items(visiblePlans) { plan ->
                val isCompleted = plan.planStatus == PlanStatus.COMPLETED
                TaskRowCard(
                    title = plan.title,
                    onOpen = { onOpen(plan) },
                    onDelete = {
                        coroutineScope.launch(Dispatchers.IO) {
                            database.flexiblePlanDao().delete(plan)
                        }
                    },
                    leading = {
                        CompletionButton(
                            isCompleted = isCompleted,
                            title = plan.title,
                            action = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val newStatus = if (isCompleted) PlanStatus.ACTIVE else PlanStatus.COMPLETED
                                    database.flexiblePlanDao().update(plan.copy(
                                        status = newStatus.name,
                                        completedAt = if (newStatus == PlanStatus.COMPLETED) System.currentTimeMillis() else null
                                    ))
                                }
                            }
                        )
                    },
                    content = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = plan.title,
                                style = AppTheme.typography.rowTitle,
                                color = if (isCompleted) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (plan.planStatus == PlanStatus.PAUSED) {
                                Text(
                                    text = "Paused",
                                    style = AppTheme.typography.caption,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
