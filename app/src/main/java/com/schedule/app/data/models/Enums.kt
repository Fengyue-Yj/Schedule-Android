package com.schedule.app.data.models

enum class WeekPattern {
    ALL, ODD, EVEN;

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class TermSeason {
    SPRING, FALL;

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.uppercase() }
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
