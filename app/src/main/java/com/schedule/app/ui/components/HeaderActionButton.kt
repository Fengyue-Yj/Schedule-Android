package com.schedule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.schedule.app.ui.theme.AppTheme

@Composable
fun HeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.accent.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppTheme.colors.accent,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun HeaderActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = HeaderActionButton(
    icon = icon,
    contentDescription = title,
    onClick = onClick,
    modifier = modifier
)

@Composable
fun HeaderActionButton(
    title: String,
    systemImage: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (systemImage) {
        "plus" -> Icons.Default.Add
        "chevron.left" -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        "chevron.right" -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        else -> Icons.Default.Settings
    }
    HeaderActionButton(icon = icon, contentDescription = title, onClick = onClick, modifier = modifier)
}
