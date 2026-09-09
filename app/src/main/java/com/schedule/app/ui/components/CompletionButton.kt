package com.schedule.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.schedule.app.ui.theme.AppTheme

@Composable
fun CompletionButton(
    isCompleted: Boolean,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (isCompleted) "Reopen $title" else "Complete $title"
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = AppTheme.colors.accent,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
fun CompletionButton(
    isCompleted: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    action: () -> Unit
) = CompletionButton(
    isCompleted = isCompleted,
    title = title,
    onClick = action,
    modifier = modifier
)
