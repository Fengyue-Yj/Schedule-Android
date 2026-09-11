package com.schedule.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.FlexiblePlanEntity
import com.schedule.app.data.models.PlanStatus
import com.schedule.app.data.models.PlanStepEntity
import com.schedule.app.data.models.PlanWindow
import com.schedule.app.ui.components.*
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
    val steps = remember { mutableStateListOf<String>() }

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        IosSheetHeader(
            title = "新建计划",
            leftActionText = "取消",
            onLeftAction = onDismiss,
            rightActionText = "保存",
            onRightAction = {
                coroutineScope.launch(Dispatchers.IO) {
                    val planId = UUID.randomUUID().toString()
                    val validSteps = steps.map { it.trim() }.filter { it.isNotEmpty() }
                    val firstStep = validSteps.firstOrNull() ?: ""

                    database.flexiblePlanDao().insert(
                        FlexiblePlanEntity(
                            id = planId,
                            title = title.trim(),
                            nextStep = firstStep,
                            window = PlanWindow.ANYTIME.name,
                            status = PlanStatus.ACTIVE.name,
                            termId = termId,
                            createdAt = System.currentTimeMillis()
                        )
                    )

                    validSteps.forEachIndexed { index, stepTitle ->
                        database.planStepDao().insert(
                            PlanStepEntity(
                                id = UUID.randomUUID().toString(),
                                planId = planId,
                                title = stepTitle,
                                isCompleted = false,
                                position = index
                            )
                        )
                    }

                    onSave()
                    onDismiss()
                }
            },
            rightActionEnabled = title.isNotBlank()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IosFormSection(headerText = "计划名称") {
                IosFormTextFieldRow(
                    label = "计划名称",
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "例如：完成机器学习课程大程"
                )
            }

            IosFormSection(
                headerText = "计划执行步骤 (可自主添加多步)",
                footerText = if (steps.isEmpty()) "点击下方按钮添加分解小步骤，每一步都更容易完成。" else null
            ) {
                if (steps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "尚未添加小步骤",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    steps.forEachIndexed { index, stepText ->
                        if (index > 0) {
                            IosFormDivider()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.accent,
                                modifier = Modifier.width(28.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                IosFormTextFieldRow(
                                    label = "",
                                    value = stepText,
                                    onValueChange = { steps[index] = it },
                                    placeholder = "步骤内容，例如：查阅 3 篇文献",
                                    modifier = Modifier.padding(vertical = 0.dp)
                                )
                            }
                            IconButton(
                                onClick = { steps.removeAt(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "删除步骤",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                IosFormDivider(startIndent = false)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosPressable { steps.add("") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "添加分解小步骤",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = AppTheme.colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
