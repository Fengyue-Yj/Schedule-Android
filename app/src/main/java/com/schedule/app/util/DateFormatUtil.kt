package com.schedule.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatUtil {
    private val isChinese: Boolean
        get() = Locale.getDefault().language.startsWith("zh")

    /**
     * Format a date into month and day:
     * - Chinese: "9月11日" or "2024年9月11日" (if different year)
     * - English: "Sep 11" or "Sep 11, 2024" (if different year)
     */
    fun formatMonthDay(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()
        val sameYear = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        
        return if (isChinese) {
            if (sameYear) {
                SimpleDateFormat("M月d日", Locale.CHINA).format(Date(millis))
            } else {
                SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(millis))
            }
        } else {
            if (sameYear) {
                SimpleDateFormat("MMM d", Locale.US).format(Date(millis))
            } else {
                SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(millis))
            }
        }
    }

    /**
     * Format a date and time:
     * - Chinese: "9月11日 14:00" or "2024年9月11日 14:00" (if different year)
     * - English: "Sep 11, 14:00" or "Sep 11, 2024, 14:00" (if different year)
     */
    fun formatDateTime(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()
        val sameYear = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        
        return if (isChinese) {
            if (sameYear) {
                SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(millis))
            } else {
                SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA).format(Date(millis))
            }
        } else {
            if (sameYear) {
                SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(millis))
            } else {
                SimpleDateFormat("MMM d, yyyy, HH:mm", Locale.US).format(Date(millis))
            }
        }
    }

    /**
     * Format a date only:
     * - Chinese: "2024年9月11日" or "9月11日"
     * - English: "Sep 11, 2024"
     */
    fun formatDate(millis: Long, includeYear: Boolean = false): String {
        return if (includeYear) {
            if (isChinese) {
                SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(millis))
            } else {
                SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(millis))
            }
        } else {
            formatMonthDay(millis)
        }
    }
}
