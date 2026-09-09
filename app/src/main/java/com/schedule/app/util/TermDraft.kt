package com.schedule.app.util

import com.schedule.app.data.models.SettingEntity
import com.schedule.app.data.models.TermSeason
import java.util.Calendar

data class TermDraft(
    var name: String = "",
    var startDate: Long = 0,
    var totalWeeks: Int = 20,
    var grade: String = "",
    var season: TermSeason = TermSeason.FALL,
    var showWeekends: Boolean = true
) {
    constructor(setting: SettingEntity) : this(
        name = setting.name.ifEmpty { setting.displayName },
        startDate = setting.termStartDate,
        totalWeeks = setting.totalWeeks,
        grade = setting.grade,
        season = try { TermSeason.valueOf(setting.termSeason) } catch (e: Exception) { TermSeason.SPRING },
        showWeekends = setting.showWeekends
    )

    constructor() : this(
        name = "",
        startDate = CalendarManager.mondayOnOrBefore(System.currentTimeMillis()),
        totalWeeks = 20,
        grade = "",
        season = if (Calendar.getInstance().get(Calendar.MONTH) >= Calendar.JULY) TermSeason.FALL else TermSeason.SPRING,
        showWeekends = true
    ) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val year = calendar.get(Calendar.YEAR)
        name = "$year ${season.displayName}"
    }

    fun apply(setting: SettingEntity): SettingEntity {
        return setting.copy(
            name = name.trim(),
            termStartDate = CalendarManager.mondayOnOrBefore(startDate),
            totalWeeks = totalWeeks,
            grade = grade.trim(),
            termSeason = season.name,
            showWeekends = showWeekends
        )
    }

    fun makeSetting(): SettingEntity {
        val setting = SettingEntity.defaultSetting()
        return apply(setting)
    }
}
