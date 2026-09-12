package com.schedule.app.teaching

import java.net.URL
import java.util.Date

enum class TeachingKind(val title: String) {
    GRADE("成绩"),
    ASSIGNMENT("作业"),
    MATERIAL("资料课件"),
    ANNOUNCEMENT("通知")
}

data class TeachingCourse(
    val id: String,
    val title: String,
    val isCurrent: Boolean,
) {
    val displayTitle: String
        get() = readableTitle(title)

    companion object {
        /**
         * Cleans PKU Teaching Network raw course titles to extract ONLY the course name.
         *
         * Raw titles often include long academic year indicators, course codes, teaching task IDs, and term suffixes:
         * - "(2024-2025-2)-04832540-0007802832-1:高等代数(I)(25-26学年第2学期)" -> "高等代数(I)"
         * - "(2024-2025-2)-04832540-0007802832-1: 高等代数" -> "高等代数"
         * - "04832540-0007802832-1: 高等代数(I)" -> "高等代数(I)"
         * - "2024-2025-2-04832540-0007802832-1: 高等代数" -> "高等代数"
         * - "(2024-2025学年第二学期)-04830110-01: 算法设计与分析" -> "算法设计与分析"
         * - "[2024-2025-2]-04832540-1: 操作系统" -> "操作系统"
         * - "04830110: 软件工程导论(01班)" -> "软件工程导论"
         * - "04832540-0007802832-1 高等代数" -> "高等代数"
         */
        fun readableTitle(raw: String): String {
            if (raw.isBlank()) return ""
            var s = raw.trim()

            // 1. If there is a colon (: or ：), check if the text before the colon is a course code / semester / ID prefix.
            val colonIdx = s.indexOfFirst { it == ':' || it == '：' }
            if (colonIdx > 0) {
                val prefix = s.substring(0, colonIdx).trim()
                val hasDigits = prefix.any { it.isDigit() }
                val hasSemesterKeyword = prefix.contains("学年") || prefix.contains("学期") || prefix.contains("春") || prefix.contains("秋")
                if (hasDigits || hasSemesterKeyword) {
                    s = s.substring(colonIdx + 1).trim()
                }
            }

            // 2. Strip bracketed semester/academic year prefix if still present
            // e.g. "(2024-2025-2)-", "[2024-2025-2]", "【2024-2025学年第二学期】", "(25-26-2)", "(2025春)"
            s = s.replace(
                Regex("""^[(\[（【][^()\[\]（）【}]*(?:\d{2,4}-\d{2,4}|\d{2}-\d{2}|学年|学期|春|秋|夏|冬|semester|term)[^()\[\]（）【}]*[)\]）】][\s\-_:：]*""", RegexOption.IGNORE_CASE),
                ""
            )

            // 3. Strip course code / numeric ID prefix if separated by whitespace, dash, or underscore
            // e.g. "04832540-0007802832-1 高等代数", "04832540-1-高等代数", "-04832540-0007802832-1-高等代数", "04832540_0007802832_1_高等代数"
            s = s.replace(Regex("""^[-_\s]*[a-zA-Z0-9]*\d{4,}[a-zA-Z0-9_-]*[\s\-_:：]+"""), "")

            // 4. Strip semester / academic year suffix at the end of the course name
            // e.g. "(25-26学年第2学期)", "(2024-2025学年第二学期)", "(2024-2025-2)", "(24-25秋)", "(2025春)"
            s = s.replace(
                Regex("""[\s\-_]*[(\[（【][^()\[\]（）【}]*(?:学年|学期|\d{2,4}-\d{2,4}|\d{2}-\d{2}|春季?|秋季?|夏季?|冬季?)[^()\[\]（）【}]*[)\]）】]\s*$"""),
                ""
            )

            // 5. Strip class / section suffix if at the very end
            // e.g. "-01班", "(01班)", "（01班）", " 01班", "_01班"
            s = s.replace(Regex("""[\s\-_]*[(\[（【]?\d{1,3}班[)\]）】]?\s*$"""), "")

            // 6. Clean any remaining leading/trailing punctuation and whitespace
            s = s.trim().trim('-', '_', ':', '：', ' ')

            return if (s.isNotEmpty()) s else raw.trim()
        }
    }
}

data class TeachingAttachment(
    val name: String,
    val url: String
)

data class ContentPage(
    val items: List<TeachingItem> = emptyList(),
    val folders: List<String> = emptyList()
)

data class TeachingMetadataPage(
    val results: List<TeachingContentMetadata> = emptyList(),
    val paging: Paging? = null
) {
    data class Paging(val nextPage: String? = null)
}

data class TeachingContentMetadata(
    val id: String,
    val title: String? = null,
    val body: String? = null,
    val created: String? = null,
    val modified: String? = null,
    val hasChildren: Boolean? = null
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
    val attachments: List<TeachingAttachment> = emptyList(),
    val readKey: String = "",
    val score: String? = null,
    val pointsPossible: String? = null,
    val gradeCategory: String? = null,
    val feedback: String? = null,
    val gradeStatus: String? = null
) {
    val displayCourseTitle: String
        get() = TeachingCourse.readableTitle(courseTitle)

    val itemReadKey: String
        get() = if (readKey.isNotEmpty()) readKey else "$id:${publishedText ?: ""}:${body.take(120).hashCode()}"

    companion object {
        fun newestFirst(items: List<TeachingItem>): List<TeachingItem> {
            return items.sortedWith { a, b ->
                when {
                    a.publishedAt != null && b.publishedAt != null -> b.publishedAt.compareTo(a.publishedAt)
                    a.publishedAt != null -> -1
                    b.publishedAt != null -> 1
                    else -> 0
                }
            }
        }
    }
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
    fun announcements(courseId: String) = course(courseId)
    fun announcementsSimple(courseId: String) = "$origin/webapps/blackboard/execute/announcement?method=search&course_id=$courseId"
    fun announcementsApi(courseId: String) = "$origin/learn/api/public/v1/courses/$courseId/announcements?limit=100"
    fun content(courseId: String, contentId: String) = "$origin/webapps/blackboard/content/listContent.jsp?course_id=$courseId&content_id=$contentId"
    fun assignment(courseId: String, contentId: String) = "$origin/webapps/assignment/uploadAssignment?course_id=$courseId&content_id=$contentId"
    fun grades(courseId: String) = "$origin/webapps/bb-mygrades-BBLEARN/myGrades.jsp?course_id=$courseId&stream_name=mygrades"
    fun gradesApi(courseId: String) = "$origin/learn/api/public/v2/courses/$courseId/gradebook/users/me"
    fun gradebookColumnsApi(courseId: String) = "$origin/learn/api/public/v2/courses/$courseId/gradebook/columns"
    
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
