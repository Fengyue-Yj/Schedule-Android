package com.schedule.app

import android.app.Application
import com.schedule.app.data.AppDatabase

class ScheduleApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
