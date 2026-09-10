package com.schedule.app.teaching

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

object TeachingParser {
    private fun isLogin(html: String): Boolean {
        return html.contains("id=\"loginForm\"") || html.contains("name=\"password\"") || html.contains("iaaa.pku.edu.cn/iaaa/oauth.jsp")
    }

    fun parseCourses(html: String): List<TeachingCourse> {
        if (isLogin(html)) throw TeachingError.LoginRequired
        val doc = Jsoup.parse(html)
        val result = mutableListOf<TeachingCourse>()
        val seen = mutableSetOf<String>()

        val portlets = doc.select("div.portlet")
        for (portlet in portlets) {
            val titleText = portlet.select("span.moduleTitle, h2, h3").text()
            val current = titleText.contains("当前") || titleText.contains("Current", ignoreCase = true) || titleText.contains("本学期") || titleText.contains("在读")
            for (link in portlet.select("ul.courseListing li a, a[href*='course_id='], a[href*='key=']")) {
                val href = link.attr("href")
                val idMatch = Regex("""(?:key=|course_id=)([\d_]+)""").find(href)
                var id = idMatch?.groupValues?.get(1)
                if (id == null) {
                    val url = TeachingURLs.resolve(href)
                    if (url != null) {
                        id = Regex("""(?:key=|course_id=)([\d_]+)""").find(url)?.groupValues?.get(1)
                    }
                }
                val courseTitle = link.text().trim()
                if (!id.isNullOrEmpty() && courseTitle.isNotEmpty() && seen.add(id)) {
                    result.add(TeachingCourse(id, courseTitle, current))
                }
            }
        }

        // Fallback: search anywhere in document if portlets missed courses
        if (result.isEmpty()) {
            for (link in doc.select("ul.courseListing li a, a[href*='course_id='], a[href*='key=']")) {
                val href = link.attr("href")
                val idMatch = Regex("""(?:key=|course_id=)([\d_]+)""").find(href)
                val id = idMatch?.groupValues?.get(1) ?: TeachingURLs.resolve(href)?.let {
                    Regex("""(?:key=|course_id=)([\d_]+)""").find(it)?.groupValues?.get(1)
                }
                val courseTitle = link.text().trim()
                if (!id.isNullOrEmpty() && courseTitle.isNotEmpty() && seen.add(id)) {
                    result.add(TeachingCourse(id, courseTitle, true))
                }
            }
        }

        return result
    }

    fun cleanHtmlToText(element: Element?): String {
        if (element == null) return ""
        val clone = element.clone()
        clone.select("br").append("\\n")
        clone.select("p").prepend("\\n\\n")
        clone.select("div").prepend("\\n")
        clone.select("li").prepend("\\n• ")
        return clone.text().replace("\\n", "\n").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    fun parseAnnouncementUrl(html: String, courseId: String): String? {
        val doc = Jsoup.parse(html)
        val link = doc.select("#courseMenuPalette_contents li a, #courseMenuPalette_div li a, a.courseMenuLink").firstOrNull {
            val text = it.text().trim()
            val href = it.attr("href")
            text.contains("公告") || text.contains("通知") || text.contains("Announcement", ignoreCase = true) || href.contains("announcement")
        }
        return link?.attr("href")?.let { TeachingURLs.resolve(it) }
    }

    fun parseAnnouncements(html: String, course: TeachingCourse): List<TeachingItem> {
        if (isLogin(html)) throw TeachingError.LoginRequired
        val doc = Jsoup.parse(html)
        val result = mutableListOf<TeachingItem>()
        val seen = mutableSetOf<String>()

        for (heading in doc.select("#announcementList h3, #content_listContainer h3, .announcement h3, .vtbegenerated h3")) {
            val title = heading.text().trim()
            if (title.isEmpty() || title == "公告" || title == "Announcements") continue
            val parent = heading.parent() ?: heading
            var content = ""
            var published = ""
            var next = heading.nextElementSibling()
            var count = 0
            while (next != null && next.tagName() != "h3" && count < 20) {
                val text = next.text().trim()
                if (text.contains("发布") || text.contains("posted on", ignoreCase = true)) {
                    published = text
                } else if (text.isNotEmpty()) {
                    val formatted = cleanHtmlToText(next)
                    content += if (content.isEmpty()) formatted else "\n$formatted"
                }
                next = next.nextElementSibling()
                count++
            }
            if (content.isEmpty()) {
                content = cleanHtmlToText(parent.select(".vtbegenerated, .details").firstOrNull() ?: parent)
            }
            val rawID = if (heading.id().isEmpty()) (if (parent.tagName() == "li") parent.id() else "") else heading.id()
            val id = if (rawID.isEmpty()) TeachingURLs.digest(title + published) else rawID
            if (seen.add(id)) {
                val publishedAtDate = parseDate(published)
                result.add(
                    TeachingItem(
                        id = "${course.id}:notice:$id",
                        courseID = course.id,
                        courseTitle = course.title,
                        contentID = id,
                        kind = TeachingKind.ANNOUNCEMENT,
                        title = title,
                        body = content,
                        publishedText = published.takeIf { it.isNotEmpty() },
                        publishedAt = publishedAtDate?.time,
                        sourceURL = TeachingURLs.course(course.id),
                        attachments = parseAttachments(parent)
                    )
                )
            }
        }
        return result
    }

    fun parseRoots(html: String): List<String> {
        if (isLogin(html)) throw TeachingError.LoginRequired
        val doc = Jsoup.parse(html)
        val links = doc.select("#courseMenuPalette_contents li a, #courseMenuPalette_div li a, a.courseMenuLink, a[href*='content_id='], a[href*='listContent']")
        val roots = mutableSetOf<String>()
        for (link in links) {
            val href = link.attr("href")
            val contentId = Regex("""content_id=([0-9_]+)""").find(href)?.groupValues?.get(1)
                ?: TeachingURLs.resolve(href)?.let { Regex("""content_id=([0-9_]+)""").find(it)?.groupValues?.get(1) }
            if (!contentId.isNullOrEmpty()) {
                roots.add(contentId)
            }
        }
        return roots.toList().sorted()
    }

    fun parseAssignments(html: String, course: TeachingCourse): List<TeachingItem> {
        return parseContents(html, course).items.filter { it.kind == TeachingKind.ASSIGNMENT }
    }

    fun parseMaterials(html: String, course: TeachingCourse): List<TeachingItem> {
        return parseContents(html, course).items.filter { it.kind == TeachingKind.MATERIAL }
    }

    fun parseContents(html: String, course: TeachingCourse): ContentPage {
        if (isLogin(html)) throw TeachingError.LoginRequired
        val doc = Jsoup.parse(html)
        val items = mutableListOf<TeachingItem>()
        val folders = mutableListOf<String>()
        
        for (row in doc.select("#content_listContainer > li")) {
            val children = row.children()
            if (children.size < 2) continue
            val image = row.select("img").firstOrNull()
            val alt = image?.attr("alt")?.lowercase() ?: ""
            val titleElement = row.select(".item, h3").firstOrNull() ?: children[1]
            val title = titleElement.text().trim()
            val titleLink = titleElement.select("a").firstOrNull()
            val url = titleLink?.attr("href")?.let { TeachingURLs.resolve(it) }
            val headerID = titleElement.id()
            val rowID = row.id()
            val id = url?.let { Regex("""content_id=([0-9_]+)""").find(it)?.groupValues?.get(1) }
                ?: (if (headerID.isNotEmpty()) headerID else rowID)
            if (id.isEmpty() || title.isEmpty()) continue
            
            val isFolder = alt.contains("文件夹") || alt.contains("folder") || (url != null && url.contains("listContent.jsp"))
            if (isFolder) {
                val folderId = url?.let { Regex("""content_id=([0-9_]+)""").find(it)?.groupValues?.get(1) } ?: id
                folders.add(folderId)
                continue
            }
            
            val assignment = alt.contains("作业") || alt.contains("assignment") || (url != null && url.contains("uploadAssignment"))
            val detail = if (children.size > 2) children[2] else row
            val body = cleanHtmlToText(detail.select(".vtbegenerated").firstOrNull() ?: detail)
            val attachments = parseAttachments(row).toMutableList()
            if (alt == "文件" || alt == "file") {
                if (attachments.isEmpty()) {
                    if (url != null && (url.contains("bbcswebdav") || url.contains("/content/file") || url.contains("launchLink"))) {
                        attachments.add(TeachingAttachment(title, url))
                    } else {
                        attachments.add(0, TeachingAttachment(title, TeachingURLs.origin + "/webapps/blackboard/execute/content/file?course_id=${course.id}&content_id=$id&mode=view"))
                    }
                }
            }
            if (!assignment && attachments.isEmpty() && url != null && (url.contains("bbcswebdav") || url.contains("/content/file") || url.contains("launchLink"))) {
                attachments.add(TeachingAttachment(title, url))
            }
            
            items.add(
                TeachingItem(
                    id = "${course.id}:$id",
                    courseID = course.id,
                    courseTitle = course.title,
                    contentID = id,
                    kind = if (assignment) TeachingKind.ASSIGNMENT else TeachingKind.MATERIAL,
                    title = title,
                    body = body,
                    sourceURL = if (assignment) TeachingURLs.assignment(course.id, id) else (url ?: TeachingURLs.content(course.id, id)),
                    attachments = attachments
                )
            )
        }
        return ContentPage(items = items, folders = folders)
    }

    fun parseDeadline(html: String): Pair<Long?, String?> {
        if (isLogin(html)) throw TeachingError.LoginRequired
        val doc = Jsoup.parse(html)
        val text = doc.select("#assignMeta2 + div").firstOrNull()?.text()?.trim()
        val date = text?.let { parseDate(it)?.time }
        return Pair(date, text)
    }

    private fun parseAttachments(element: Element): List<TeachingAttachment> {
        val attachments = mutableListOf<TeachingAttachment>()
        val seen = mutableSetOf<String>()
        for (a in element.select("ul.attachments a, audio + ul a, a[href*='bbcswebdav'], a[href*='/content/file'], a[href*='launchLink'], a[href*='download']")) {
            val href = a.attr("href")
            val url = TeachingURLs.resolve(href) ?: continue
            if (seen.add(url)) {
                var name = a.text().trim()
                if (name.isEmpty()) name = TeachingURLs.filename(url)
                attachments.add(TeachingAttachment(name, url))
            }
        }
        return attachments
    }

    fun parseDate(text: String): Date? {
        val normalized = text.replace("\u00a0", " ").trim()
        // Chinese pattern: 2024年10月15日 星期二 下午 23:59 or 2024年10月15日 下午11:59
        val chineseRegex = Regex("""(\d{4})年(\d{1,2})月(\d{1,2})日\s*(?:星期\S\s*)?(上午|下午)?\s*(\d{1,2}):(\d{2})""")
        val match = chineseRegex.find(normalized)
        if (match != null) {
            try {
                val year = match.groupValues[1].toInt()
                val month = match.groupValues[2].toInt()
                val day = match.groupValues[3].toInt()
                val ampm = match.groupValues[4]
                var hour = match.groupValues[5].toInt()
                val minute = match.groupValues[6].toInt()
                if (ampm == "下午" && hour < 12) hour += 12
                if (ampm == "上午" && hour == 12) hour = 0
                val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
                cal.set(year, month - 1, day, hour, minute, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                return cal.time
            } catch (e: Exception) {
                // Ignore
            }
        }
        val formats = listOf(
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "EEEE, MMMM d, yyyy h:mm a",
            "MMMM d, yyyy h:mm a",
            "MMM d, yyyy h:mm a"
        )
        for (format in formats) {
            val sdf = SimpleDateFormat(format, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
            try {
                return sdf.parse(normalized)
            } catch (e: Exception) {
                // Ignore
            }
        }
        return null
    }
}
