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

data class ContentPage(
    val items: List<TeachingItem> = emptyList(),
    val folders: List<String> = emptyList()
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

    val itemReadKey: String
        get() = if (readKey.isNotEmpty()) readKey else "$id:${publishedText ?: ""}:${body.take(120).hashCode()}"
}

data class TeachingSnapshot(
    val courses: List<TeachingCourse> = emptyList(),
    val items: List<TeachingItem> = emptyList(),
    val fetchedAt: Long = System.currentTimeMillis(),
    val readKeys: Set<String> = emptySet(),
    val courseLinks: Map<String, String> = emptyMap() // courseID -> localCourseID
)

object TeachingURLs {
    const val origin = "https://course.pku.edu.cn"
    const val login = "https://course.pku.edu.cn/webapps/bb-sso-BBLEARN/login.html"
    const val home = "$origin/webapps/portal/execute/tabs/tabAction?tab_tab_group_id=_1_1"
    const val me = "$origin/learn/api/public/v1/users/me"

    fun course(courseId: String) = "$origin/webapps/blackboard/execute/announcement?method=search&context=course_entry&course_id=$courseId&handle=announcements_entry&mode=view"
    fun content(courseId: String, contentId: String) = "$origin/webapps/blackboard/content/listContent.jsp?course_id=$courseId&content_id=$contentId"
    fun assignment(courseId: String, contentId: String) = "$origin/webapps/assignment/uploadAssignment?action=newAttempt&course_id=$courseId&content_id=$contentId"
    
    fun trusted(url: String): Boolean {
        val uri = try { java.net.URI(url) } catch (e: Exception) { return false }
        val host = uri.host?.lowercase() ?: return false
        return host == "pku.edu.cn" || host.endsWith(".pku.edu.cn")
    }

    fun resolve(href: String): String? {
        val cleaned = href.trim().replace("@X@EmbeddedFile.requestUrlStub@X@", "/")
        if (cleaned.isEmpty() || cleaned.startsWith("#")) return null
        val fullUrl = if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            cleaned
        } else if (cleaned.startsWith("/")) {
            "$origin$cleaned"
        } else {
            "$origin/$cleaned"
        }
        val uri = try { java.net.URI(fullUrl) } catch (e: Exception) { return null }
        val host = uri.host?.lowercase() ?: return null
        if (host != "pku.edu.cn" && !host.endsWith(".pku.edu.cn")) return null
        if (uri.scheme?.equals("http", ignoreCase = true) == true) {
            return fullUrl.replaceFirst("http://", "https://")
        }
        return fullUrl
    }

    fun digest(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }

    fun filename(url: String): String {
        return try {
            URL(url).path.substringAfterLast("/")
        } catch (e: Exception) {
            "attachment"
        }
    }
}

sealed class TeachingError(message: String) : Exception(message) {
    object LoginRequired : TeachingError("登录已过期，请重新连接教学网")
    object UnexpectedPage : TeachingError("教学网页面结构未能识别")
    object FileUnavailable : TeachingError("教学网返回了网页而非文件内容，请在原页面查看")
    object UntrustedURL : TeachingError("链接不在北大受信任域名内")
    class NetworkError(message: String) : TeachingError(message)
}
