package com.schedule.app.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.UUID

@Entity(
    tableName = "flexible_plans",
    indices = [Index("termId"), Index("courseId")]
)
data class FlexiblePlanEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val nextStep: String = "",
    val completionGoal: String = "",
    val notes: String = "",
    val window: String = PlanWindow.SOON.name,
    val weekAnchor: Long? = null,
    val status: String = PlanStatus.ACTIVE.name,
    val termId: String? = null,
    val courseId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val planStatus: PlanStatus
        get() = try { PlanStatus.valueOf(status) } catch (e: Exception) { PlanStatus.ACTIVE }

    val planWindow: PlanWindow
        get() = try { PlanWindow.valueOf(window) } catch (e: Exception) { PlanWindow.SOON }

    fun needsReview(onDate: Long = System.currentTimeMillis()): Boolean {
        if (planWindow != PlanWindow.THIS_WEEK || planStatus != PlanStatus.ACTIVE || weekAnchor == null) {
            return false
        }
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = weekAnchor
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val end = calendar.timeInMillis
        
        val today = Calendar.getInstance()
        today.timeInMillis = onDate
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        return today.timeInMillis >= end
    }

    fun rescheduleToThisWeek(onDate: Long = System.currentTimeMillis()): FlexiblePlanEntity {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = onDate
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val diff = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        if (diff > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -diff)
        }
        
        return this.copy(
            window = PlanWindow.THIS_WEEK.name,
            weekAnchor = calendar.timeInMillis,
            status = PlanStatus.ACTIVE.name,
            completedAt = null
        )
    }
}
