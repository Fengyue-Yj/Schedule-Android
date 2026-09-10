package com.schedule.app.util

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

object ColorUtil {
    fun hexToColor(hex: String): Color? {
        val trimmed = hex.trim()
        val hexString = if (trimmed.startsWith("#")) trimmed.substring(1) else trimmed
        
        if (hexString.length != 6) return null
        
        return try {
            val value = hexString.toInt(16)
            val red = (value shr 16) and 0xFF
            val green = (value shr 8) and 0xFF
            val blue = value and 0xFF
            Color(red, green, blue)
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun colorToHex(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    fun stableIndex(value: String, count: Int): Int {
        if (count <= 0) return 0
        var hash = 5381
        for (char in value) {
            hash = ((hash shl 5) + hash) + char.code
        }
        return (hash.absoluteValue % count)
    }

    val lightPalette = listOf(
        Color(0xFFDBF5E6),
        Color(0xFFCCEBD8),
        Color(0xFFE6FAF0),
        Color(0xFFC7E6CC),
        Color(0xFFEBF5E0),
        Color(0xFFD6F0EB)
    )

    val darkPalette = listOf(
        Color(0xFF1A3D2E),
        Color(0xFF1F4733),
        Color(0xFF143829),
        Color(0xFF244D38),
        Color(0xFF173326),
        Color(0xFF214230)
    )

    fun displayColor(colorHex: String?, colorSeed: Int, id: String, isDark: Boolean): Color {
        if (!colorHex.isNullOrEmpty()) {
            val custom = hexToColor(colorHex)
            if (custom != null) return custom
        }
        val palette = if (isDark) darkPalette else lightPalette
        val index = if (colorSeed > 0) {
            colorSeed % palette.size
        } else {
            stableIndex(id, palette.size)
        }
        return palette[index]
    }
}
