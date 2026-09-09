package com.schedule.app.data.models

import androidx.room.Embedded
import androidx.room.Relation

data class CourseWithMeetings(
    @Embedded val course: CourseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val meetings: List<CourseMeetingEntity>
)

data class CourseWithAll(
    @Embedded val course: CourseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val meetings: List<CourseMeetingEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val assignments: List<AssignmentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val exams: List<ExamEntity>
)

data class FlexiblePlanWithSteps(
    @Embedded val plan: FlexiblePlanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "planId"
    )
    val steps: List<PlanStepEntity>
)
