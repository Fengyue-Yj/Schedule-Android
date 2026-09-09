package com.schedule.app.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "plan_steps",
    foreignKeys = [
        ForeignKey(
            entity = FlexiblePlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class PlanStepEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0
)
