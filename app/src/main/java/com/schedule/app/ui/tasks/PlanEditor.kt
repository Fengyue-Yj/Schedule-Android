package com.schedule.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.FlexiblePlanEntity
import com.schedule.app.data.models.PlanStatus
import com.schedule.app.data.models.PlanWindow
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditor(
    database: AppDatabase,
    termId: String? = null,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var nextStep by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("New Plan") },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                database.flexiblePlanDao().insert(
                                    FlexiblePlanEntity(
                                        id = UUID.randomUUID().toString(),
                                        title = title.trim(),
                                        nextStep = nextStep.trim(),
                                        window = PlanWindow.ANYTIME.name,
                                        status = PlanStatus.ACTIVE.name,
                                        termId = termId,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                                onSave()
                                onDismiss()
                            }
                        },
                        enabled = title.trim().isNotEmpty()
                    ) { Text("Save") }
                }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppTheme.Spacing.page),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Plan Title") },
                    placeholder = { Text("e.g. Finish Data Structures Project") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nextStep,
                    onValueChange = { nextStep = it },
                    label = { Text("Next Small Step (Optional)") },
                    placeholder = { Text("e.g. Write test cases for AVL Tree") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
