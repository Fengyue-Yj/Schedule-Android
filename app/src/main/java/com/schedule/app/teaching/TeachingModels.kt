package com.schedule.app.teaching

import java.net.URL
import java.util.Date

enum class TeachingKind(val title: String) {
    ANNOUNCEMENT("Notices"),
    ASSIGNMENT("Assignments"),
    MATERIAL("Materials")
}

data class TeachingCourse(
    val id: String,
    val title: String,
    val isCurrent: Boolean,
) {
    val displayTitle: String
        get() = title.replace(Regex("\\(.*?\\)"), "").trim()
}

data class TeachingAttachment(
    val name: String,
    val url: String
)

data class TeachingItem(
    val id: String,
    val courseID: String,
    val courseTitle: String,
    val contentID: String,
    val kind: TeachingKind,
    val title: String,
    val body: String,
    val dueDate: Long? = null,
    val dueDateText: String? = null,
    val publishedText: String? = null,
    val publishedAt: Long? = null,
    val sourceURL: String,
    val attachments: List<TeachingAttachment>,
    val readKey: String = "",
) {
    val displayCourseTitle: String
        get() = courseTitle.replace(Regex("\\(.*?\\)"), "").trim()
}

data class TeachingSnapshot(
    val courses: List<TeachingCourse> = emptyList(),
    val items: List<TeachingItem> = emptyList(),
    val fetchedAt: Long = System.currentTimeMillis(),
    val readKeys: Set<String> = emptySet(),
    val courseLinks: Map<String, String> = emptyMap() // courseID -> localCourseID
)

object TeachingURLs {
    val origin = "https://course.pku.edu.cn"
    val login = "https://iaaa.pku.edu.cn/iaaa/oauth.jsp"
    val home = "$origin/webapps/portal/execute/tabs/tabAction?tab_tab_group_id=_1_1"
    val me = "$origin/webapps/blackboard/execute/editMe"

    fun course(courseId: String) = "$origin/webapps/blackboard/execute/modulepage/view?course_id=$courseId&cmp_tab_id=_1_1&editMode=false&mode=cpview"
    fun content(courseId: String, contentId: String) = "$origin/webapps/blackboard/content/listContent.jsp?course_id=$courseId&content_id=$contentId"
    fun assignment(courseId: String, contentId: String) = "$origin/webapps/assignment/uploadAssignment?course_id=$courseId&content_id=$contentId"
    
    fun resolve(href: String): String? {
        if (href.startsWith("http")) return href
        if (href.startsWith("/")) return "$origin$href"
        return null
    }

    fun digest(input: String): String {
        return input.hashCode().toString()
    }

    fun filename(url: String): String {
        return URL(url).path.substringAfterLast("/")
    }
}

sealed class TeachingError(message: String) : Exception(message) {
    object LoginRequired : TeachingError("Login required")
    object UnexpectedPage : TeachingError("Unexpected page format")
    class NetworkError(message: String) : TeachingError(message)
}
