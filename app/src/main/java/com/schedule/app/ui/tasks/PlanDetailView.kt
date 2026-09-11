package com.schedule.app.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.schedule.app.ui.components.*
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

    IosModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        IosSheetHeader(
            title = if (isEditing) "编辑计划" else "计划详情",
            leftActionText = if (isEditing) "取消" else "关闭",
            onLeftAction = {
                if (isEditing) {
                    title = plan.title
                    nextStep = plan.nextStep
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
                            plan.copy(
                                title = title.trim(),
                                nextStep = nextStep.trim()
                            )
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
                    IosFormDivider()
                    IosFormTextFieldRow(
                        label = "下一步行动",
                        value = nextStep,
                        onValueChange = { nextStep = it },
                        placeholder = "例如：先阅读前两章"
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

                    if (plan.nextStep.isNotBlank()) {
                        IosFormDivider()
                        IosFormRow(
                            label = "下一步",
                            value = plan.nextStep,
                            valueColor = AppTheme.colors.accent,
                            showChevron = false
                        )
                    }

                    IosFormDivider()
                    IosFormRow(
                        label = "当前状态",
                        value = plan.planStatus.title,
                        valueColor = if (plan.planStatus == PlanStatus.ACTIVE) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        showChevron = false
                    )
                }

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
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
