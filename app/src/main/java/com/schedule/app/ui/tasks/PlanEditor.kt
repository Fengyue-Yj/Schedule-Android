package com.schedule.app.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schedule.app.data.AppDatabase
import com.schedule.app.data.models.FlexiblePlanEntity
import com.schedule.app.data.models.PlanStatus
import com.schedule.app.data.models.PlanWindow
import com.schedule.app.ui.components.*
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
            rightActionEnabled = title.isNotBlank()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IosFormSection(headerText = "计划内容") {
                IosFormTextFieldRow(
                    label = "计划名称",
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "例如：数据结构期末大作业"
                )
                IosFormDivider()
                IosFormTextFieldRow(
                    label = "第一小步",
                    value = nextStep,
                    onValueChange = { nextStep = it },
                    placeholder = "选填，例如：先阅读前两章并写提纲"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
