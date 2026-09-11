package com.schedule.app.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.FlexiblePlanEntity
import com.schedule.app.data.models.PlanStatus
import com.schedule.app.data.models.PlanStepEntity
import com.schedule.app.ui.components.*
import com.schedule.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

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
    var newStepText by remember { mutableStateOf("") }

    val steps by database.planStepDao().getByPlanId(plan.id).collectAsState(initial = emptyList())

    // Helper to keep plan.nextStep updated to the first pending step
    fun syncNextStep(currentSteps: List<PlanStepEntity>) {
        val nextPending = currentSteps.firstOrNull { !it.isCompleted }?.title ?: ""
        coroutineScope.launch(Dispatchers.IO) {
            database.flexiblePlanDao().update(plan.copy(nextStep = nextPending))
        }
    }

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        IosSheetHeader(
            title = if (isEditing) "编辑计划" else "计划详情",
            leftActionText = if (isEditing) "取消" else "关闭",
            onLeftAction = {
                if (isEditing) {
                    title = plan.title
                    isEditing = false
                } else {
                    onDismiss()
                }
            },
            rightActionText = if (isEditing) "保存" else "编辑",
            onRightAction = {
                if (isEditing) {
                    coroutineScope.launch(Dispatchers.IO) {
                        database.flexiblePlanDao().update(
                            plan.copy(title = title.trim())
                        )
                        isEditing = false
                    }
                } else {
                    isEditing = true
                }
            },
            rightActionEnabled = !isEditing || title.isNotBlank()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isEditing) {
                IosFormSection(headerText = "计划信息") {
                    IosFormTextFieldRow(
                        label = "计划名称",
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "输入计划名称"
                    )
                }
            } else {
                IosFormSection {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IosFormDivider()
                    IosFormRow(
                        label = "当前状态",
                        value = plan.planStatus.title,
                        valueColor = if (plan.planStatus == PlanStatus.ACTIVE) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        showChevron = false
                    )

                    if (steps.isNotEmpty()) {
                        val completedCount = steps.count { it.isCompleted }
                        IosFormDivider()
                        IosFormRow(
                            label = "执行进度",
                            value = "$completedCount / ${steps.size} 完成",
                            valueColor = AppTheme.colors.accent,
                            showChevron = false
                        )
                    }
                }
            }

            // Steps section
            IosFormSection(
                headerText = "计划分解小步骤 (${steps.count { it.isCompleted }}/${steps.size})",
                footerText = "每个小步骤均可独立点击打勾完成，随时自由添加或删除。"
            ) {
                if (steps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无步骤，可在下方添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    steps.forEachIndexed { index, step ->
                        if (index > 0) {
                            IosFormDivider()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular completion checkbox
                            Surface(
                                modifier = Modifier
                                    .size(24.dp)
                                    .iosPressable {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            database.planStepDao().update(
                                                step.copy(isCompleted = !step.isCompleted)
                                            )
                                            val updated = steps.map {
                                                if (it.id == step.id) it.copy(isCompleted = !step.isCompleted) else it
                                            }
                                            syncNextStep(updated)
                                        }
                                    },
                                shape = CircleShape,
                                color = if (step.isCompleted) AppTheme.colors.accent else Color.Transparent,
                                border = if (step.isCompleted) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                            ) {
                                if (step.isCompleted) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (step.isCompleted) TextDecoration.LineThrough else null,
                                color = if (step.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        database.planStepDao().delete(step)
                                        val updated = steps.filter { it.id != step.id }
                                        syncNextStep(updated)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除步骤",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Add step inline row
                IosFormDivider(startIndent = false)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (newStepText.isEmpty()) {
                            Text(
                                text = "添加小步骤...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        }
                        BasicTextField(
                            value = newStepText,
                            onValueChange = { newStepText = it },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(AppTheme.colors.accent),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (newStepText.isNotBlank()) {
                        TextButton(
                            onClick = {
                                val textToAdd = newStepText.trim()
                                if (textToAdd.isNotEmpty()) {
                                    newStepText = ""
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val newStep = PlanStepEntity(
                                            id = UUID.randomUUID().toString(),
                                            planId = plan.id,
                                            title = textToAdd,
                                            isCompleted = false,
                                            position = steps.size
                                        )
                                        database.planStepDao().insert(newStep)
                                        val updated = steps + newStep
                                        syncNextStep(updated)
                                    }
                                }
                            }
                        ) {
                            Text("添加", color = AppTheme.colors.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Plan Status Toggle Button
            if (plan.planStatus != PlanStatus.COMPLETED) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .iosPressable {
                            coroutineScope.launch(Dispatchers.IO) {
                                val newStatus = if (plan.planStatus == PlanStatus.PAUSED) PlanStatus.ACTIVE else PlanStatus.PAUSED
                                database.flexiblePlanDao().update(plan.copy(status = newStatus.name))
                                onDismiss()
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.surface,
                    border = BorderStroke(0.5.dp, AppTheme.colors.border.copy(alpha = 0.4f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (plan.planStatus == PlanStatus.PAUSED) "恢复此计划" else "暂停此计划",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (plan.planStatus == PlanStatus.PAUSED) AppTheme.colors.accent else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
