package com.schedule.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.schedule.app.data.converters.Converters
import com.schedule.app.data.dao.*
import com.schedule.app.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SettingEntity::class,
        CourseEntity::class,
        CourseMeetingEntity::class,
        AssignmentEntity::class,
        ExamEntity::class,
        FlexiblePlanEntity::class,
        PlanStepEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun settingDao(): SettingDao
    abstract fun courseDao(): CourseDao
    abstract fun courseMeetingDao(): CourseMeetingDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun examDao(): ExamDao
    abstract fun flexiblePlanDao(): FlexiblePlanDao
    abstract fun planStepDao(): PlanStepDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schedule_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Create default setting on first creation
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                database.settingDao().insert(SettingEntity.defaultSetting())
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
