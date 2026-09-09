package com.schedule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.schedule.app.ui.theme.AppTheme

@Composable
fun Modifier.cardBackground(
    fill: Color = AppTheme.colors.surface,
    cornerRadius: Dp = AppTheme.Radius.card
): Modifier {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius)
    
    return this
        .shadow(
            elevation = if (isDark) 0.dp else 5.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (isDark) 0f else 0.035f),
            spotColor = Color.Black.copy(alpha = if (isDark) 0f else 0.035f)
        )
        .background(color = fill, shape = shape)
        .border(
            width = 0.7.dp,
            color = AppTheme.colors.border,
            shape = shape
        )
}
