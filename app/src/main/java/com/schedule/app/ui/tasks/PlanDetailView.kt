package com.schedule.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.FlexiblePlanEntity
import com.schedule.app.data.models.PlanStatus
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailView(
    plan: FlexiblePlanEntity,
    database: AppDatabase,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isEditing by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(plan.title) }
    var nextStep by remember { mutableStateOf(plan.nextStep) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("Plan") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                actions = {
                    if (isEditing) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    database.flexiblePlanDao().update(
                                        plan.copy(
                                            title = title.trim(),
                                            nextStep = nextStep.trim()
                                        )
                                    )
                                    onDismiss()
                                }
                            },
                            enabled = title.trim().isNotEmpty()
                        ) { Text("Save") }
                    } else {
                        TextButton(onClick = { isEditing = true }) { Text("Edit") }
                    }
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.Spacing.page),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nextStep,
                        onValueChange = { nextStep = it },
                        label = { Text("Next Step") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(plan.title, style = MaterialTheme.typography.headlineMedium)
                    if (plan.nextStep.isNotBlank()) {
                        Text("Next Step: ${plan.nextStep}", style = MaterialTheme.typography.bodyLarge, color = AppTheme.colors.accent)
                    }
                }
                
                Text(
                    text = "Status: ${plan.planStatus.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!isEditing && plan.planStatus != PlanStatus.COMPLETED) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val newStatus = if (plan.planStatus == PlanStatus.PAUSED) PlanStatus.ACTIVE else PlanStatus.PAUSED
                                database.flexiblePlanDao().update(plan.copy(status = newStatus.name))
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (plan.planStatus == PlanStatus.PAUSED) "Resume Plan" else "Pause Plan")
                    }
                }
            }
        }
    }
}
