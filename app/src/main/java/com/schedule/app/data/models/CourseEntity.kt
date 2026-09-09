package com.schedule.app.data.models

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlin.math.abs

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = SettingEntity::class,
            parentColumns = ["id"],
            childColumns = ["termId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("termId")]
)
data class CourseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val termId: String? = null,
    val name: String,
    val teacher: String = "",
    val classroom: String = "",
    val weekday: Int = 1,
    val startPeriod: Int = 1,
    val endPeriod: Int = 2,
    val weekPattern: String = WeekPattern.ALL.name,
    val colorHex: String = "",
    val colorSeed: Int = (1..1000000).random(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val pattern: WeekPattern
        get() = try { WeekPattern.valueOf(weekPattern) } catch (e: Exception) { WeekPattern.ALL }

    fun isActive(inWeek: Int): Boolean {
        return when (pattern) {
            WeekPattern.ALL -> true
            WeekPattern.ODD -> inWeek % 2 == 1
            WeekPattern.EVEN -> inWeek % 2 == 0
        }
    }

    fun displayColor(isDarkTheme: Boolean = false): Color {
        if (colorHex.isNotEmpty()) {
            try {
                return Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                // Ignore parse error and fallback
            }
        }
        
        val lightPalette = listOf(
            Color(0.86f, 0.96f, 0.90f),
            Color(0.80f, 0.92f, 0.86f),
            Color(0.90f, 0.98f, 0.94f),
            Color(0.78f, 0.90f, 0.80f),
            Color(0.92f, 0.96f, 0.88f),
            Color(0.84f, 0.94f, 0.92f)
        )
        val darkPalette = listOf(
            Color(0.10f, 0.24f, 0.18f),
            Color(0.12f, 0.28f, 0.20f),
            Color(0.08f, 0.22f, 0.16f),
            Color(0.14f, 0.30f, 0.22f),
            Color(0.09f, 0.20f, 0.15f),
            Color(0.13f, 0.26f, 0.19f)
        )
        
        val palette = if (isDarkTheme) darkPalette else lightPalette
        val index = if (colorSeed == 0) {
            stableIndex(id, palette.size)
        } else {
            abs(colorSeed) % palette.size.coerceAtLeast(1)
        }
        
        return palette[index]
    }

    private fun stableIndex(value: String, count: Int): Int {
        var hash = 0
        for (char in value) {
            hash = (hash * 31) + char.code
        }
        return abs(hash) % count.coerceAtLeast(1)
    }
}
