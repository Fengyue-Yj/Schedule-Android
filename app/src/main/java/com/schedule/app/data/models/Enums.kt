package com.schedule.app.data.models

enum class WeekPattern {
    ALL, ODD, EVEN;

    val displayName: String
        get() = when (this) {
            ALL -> "每周"
            ODD -> "单周"
            EVEN -> "双周"
        }
}

enum class TermSeason {
    SPRING, FALL;

    val displayName: String
        get() = when (this) {
            SPRING -> "春季"
            FALL -> "秋季"
        }

    companion object {
        fun fromString(str: String): TermSeason {
            val upper = str.uppercase().trim()
            return if (upper == "FALL" || upper.contains("秋")) FALL else SPRING
        }
    }
}

enum class PlanWindow {
    THIS_WEEK, SOON, ANYTIME;

    val title: String
        get() = when (this) {
            THIS_WEEK -> "This Week"
            SOON -> "Soon"
            ANYTIME -> "Anytime"
        }
}

enum class PlanStatus {
    ACTIVE, PAUSED, COMPLETED;

    val title: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}
