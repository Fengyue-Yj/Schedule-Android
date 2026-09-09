package com.schedule.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CompanionDialogue {
    fun lines(events: List<UpcomingEvent>, nextStep: String?, now: Long = System.currentTimeMillis()): List<String> {
        val lines = mutableListOf(
            "今天也可以先迈出很小的一步。",
            "我在这里陪你。累了就伸个懒腰吧。",
            "不用一下做完所有事，慢慢来也很好。"
        )

        events.firstOrNull()?.let { event ->
            val calendar = Calendar.getInstance()
            
            // start of today
            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis
            
            // start of event day
            calendar.timeInMillis = event.date
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val eventStart = calendar.timeInMillis
            
            val days = ((eventStart - todayStart) / (1000 * 60 * 60 * 24)).toInt()
            val name = event.title.take(45)
            
            val reminder = when {
                event.date < now -> {
                    "「$name」已经到期了，看看是否还需要处理吧。"
                }
                days == 0 -> {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "「$name」就在今天 ${timeFormat.format(event.date)}，我帮你记着呢。"
                }
                else -> {
                    "「$name」还有 $days 天${if (event.isExam) "就要考试" else "到期"}，可以先看一眼。"
                }
            }
            lines.add(0, reminder)
        }

        if (!nextStep.isNullOrBlank()) {
            lines.add("有空可以先做这一小步：${nextStep.trim().take(60)}")
        }

        return lines
    }

    fun canSpeak(lastSpoken: Long?, now: Long = System.currentTimeMillis()): Boolean {
        if (lastSpoken == null) return true
        return (now - lastSpoken) >= 25000 // 25 seconds
    }
}
