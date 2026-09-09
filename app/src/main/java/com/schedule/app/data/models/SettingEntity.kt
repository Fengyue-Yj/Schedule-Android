package com.schedule.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.UUID

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val termStartDate: Long,
    val totalWeeks: Int,
    val grade: String = "",
    val termSeason: String = TermSeason.SPRING.name,
    val showWeekends: Boolean = true,
    val periodTimeStrings: List<String> = defaultPeriods,
    val weekStartsOnMonday: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() {
            if (name.trim().isEmpty()) {
                val calendar = Calendar.getInstance().apply { timeInMillis = termStartDate }
                val year = calendar.get(Calendar.YEAR)
                val seasonName = try { TermSeason.valueOf(termSeason).displayName } catch (e: Exception) { "Spring" }
                return "$year $seasonName"
            }
            return name
        }

    val season: TermSeason
        get() = try { TermSeason.valueOf(termSeason) } catch (e: Exception) { TermSeason.SPRING }

    companion object {
        val defaultPeriods = listOf(
            "08:00-08:50",
            "09:00-09:50",
            "10:10-11:00",
            "11:10-12:00",
            "13:00-13:50",
            "14:00-14:50",
            "15:10-16:00",
            "16:10-17:00",
            "17:10-18:00",
            "18:40-19:30",
            "19:40-20:30",
            "20:40-21:30"
        )

        fun defaultSetting(): SettingEntity {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val isFall = calendar.get(Calendar.MONTH) >= Calendar.JULY
            val season = if (isFall) TermSeason.FALL else TermSeason.SPRING

            calendar.set(year, if (isFall) Calendar.SEPTEMBER else Calendar.MARCH, 1)
            
            // Adjust to Monday on or before
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val diff = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            if (diff > 0) {
                calendar.add(Calendar.DAY_OF_YEAR, -diff)
            }
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            return SettingEntity(
                termStartDate = calendar.timeInMillis,
                totalWeeks = 20,
                termSeason = season.name,
                periodTimeStrings = defaultPeriods
            )
        }
    }
}
