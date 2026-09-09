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

    private val lightPalette = listOf(
        Color(0xFF34C759),
        Color(0xFF30B0C7),
        Color(0xFF32ADE6),
        Color(0xFF007AFF),
        Color(0xFF5856D6),
        Color(0xFFAF52DE)
    )

    private val darkPalette = listOf(
        Color(0xFF30D158),
        Color(0xFF64D2FF),
        Color(0xFF0A84FF),
        Color(0xFF5E5CE6),
        Color(0xFFBF5AF2),
        Color(0xFFFF375F)
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
