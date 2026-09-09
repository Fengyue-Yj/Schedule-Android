package com.schedule.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

class AppThemeColors(
    val accent: Color,
    val background: Color,
    val surface: Color,
    val subtleSurface: Color,
    val border: Color,
    val controlFill: Color,
    val selectedFill: Color,
    val highlight: Color = Highlight,
    val deepGreen: Color = DeepGreen
) {
    val primary: Color get() = accent
}

val LocalAppColors = staticCompositionLocalOf<AppThemeColors> {
    error("No AppColors provided")
}

object AppTheme {
    val colors: AppThemeColors
        @Composable
        get() = LocalAppColors.current

    object Spacing {
        val page: Dp = 24.dp
        val card: Dp = 16.dp
        val section: Dp = 16.dp
        val row: Dp = 10.dp
        val small: Dp = 8.dp
    }
    val spacing = Spacing
    
    object Radius {
        val card: Dp = 18.dp
        val hero: Dp = 26.dp
        val compact: Dp = 10.dp
    }
    val radius = Radius
    
    val typography: androidx.compose.material3.Typography
        @Composable
        get() = MaterialTheme.typography
}

@Composable
fun ScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val accent = if (darkTheme) AccentDark else AccentLight
    val background = if (darkTheme) Color(0xFF000000) else Color(0xFFF2F2F7)
    val surface = if (darkTheme) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val subtleSurface = if (darkTheme) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val border = if (darkTheme) Color(0x2EFFFFFF) else Color(0x2E000000)
    val controlFill = if (darkTheme) Color(0x24FFFFFF) else Color(0x1F767680)
    val selectedFill = if (darkTheme) Color(0xFF294A3B) else Color(0xFFD6F0E0)

    val appColors = AppThemeColors(
        accent = accent,
        background = background,
        surface = surface,
        subtleSurface = subtleSurface,
        border = border,
        controlFill = controlFill,
        selectedFill = selectedFill,
        highlight = Highlight,
        deepGreen = DeepGreen
    )

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            background = background,
            surface = surface,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = accent,
            background = background,
            surface = surface,
            onBackground = Color.Black,
            onSurface = Color.Black
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
