package com.schedule.app.util

import com.schedule.app.data.models.AssignmentEntity
import com.schedule.app.data.models.ExamEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class UpcomingEvent(
    val id: String,
    val title: String,
    val date: Long,
    val isExam: Boolean
) {
    companion object {
        fun collect(assignments: List<AssignmentEntity>, exams: List<ExamEntity>, now: Long = System.currentTimeMillis()): List<UpcomingEvent> {
            val tasks = assignments.filter { !it.isCompleted }.map {
                UpcomingEvent(id = it.id, title = it.content, date = it.dueDate, isExam = false)
            }
            val tests = exams.filter { it.date >= now }.map {
                UpcomingEvent(id = it.id, title = it.subject, date = it.date, isExam = true)
            }
            return (tasks + tests).sortedBy { it.date }
        }
    }

    fun relativeTime(now: Long = System.currentTimeMillis()): String {
        if (date < now) return "Past due"
        
        val calendar = Calendar.getInstance()
        
        // Check if same day
        calendar.timeInMillis = now
        val nowYear = calendar.get(Calendar.YEAR)
        val nowDay = calendar.get(Calendar.DAY_OF_YEAR)
        
        calendar.timeInMillis = date
        val dateYear = calendar.get(Calendar.YEAR)
        val dateDay = calendar.get(Calendar.DAY_OF_YEAR)
        
        if (nowYear == dateYear && nowDay == dateDay) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            return "Today · ${timeFormat.format(date)}"
        }
        
        // Start of day calculations
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val nowStart = calendar.timeInMillis
        
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dateStart = calendar.timeInMillis
        
        val days = ((dateStart - nowStart) / (1000 * 60 * 60 * 24)).toInt()
        if (days == 1) return "Tomorrow"
        return "In $days days"
    }
}
