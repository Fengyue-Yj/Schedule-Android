package com.schedule.app.teaching

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import com.google.gson.Gson
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
        clone.select("br").append("\n")
        clone.select("p").prepend("\n\n")
        clone.select("div").prepend("\n")
        clone.select("li").prepend("\n• ")
        return clone.text().replace("\r", "").replace(Regex("\n{3,}"), "\n\n").trim()
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

    fun parseApiAnnouncements(json: String, course: TeachingCourse): List<TeachingItem> {
        val result = mutableListOf<TeachingItem>()
        try {
            val page = Gson().fromJson(json, TeachingMetadataPage::class.java) ?: return emptyList()
            for (row in page.results) {
                val title = row.title?.trim()
                if (title.isNullOrEmpty() || title == "公告" || title == "Announcements" || title == "课程公告") continue
                val bodyDoc = Jsoup.parse(row.body ?: "")
                val bodyText = cleanHtmlToText(bodyDoc.body() ?: bodyDoc)
                val publishedAtDate = row.created?.let { parseDate(it) }
                result.add(
                    TeachingItem(
                        id = "${course.id}:notice:${row.id}",
                        courseID = course.id,
                        courseTitle = course.title,
                        contentID = row.id,
                        kind = TeachingKind.ANNOUNCEMENT,
                        title = title,
                        body = bodyText,
                        publishedText = row.created,
                        publishedAt = publishedAtDate?.time,
                        sourceURL = TeachingURLs.course(course.id),
                        attachments = parseAttachments(bodyDoc)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun parseAnnouncements(html: String, course: TeachingCourse): List<TeachingItem> {
        if (isLogin(html)) throw TeachingError.LoginRequired
        val doc = Jsoup.parse(html)
        val result = mutableListOf<TeachingItem>()
        val seen = mutableSetOf<String>()

        // Strategy 1: Container-level selectors (modern & classic Blackboard announcement lists)
        val containers = doc.select("#announcementList > li, ul.announcementList > li, #content_listContainer > li, .announcement, div.announcementItem, .announcementEntry")
        for (container in containers) {
            val titleEl = container.select("h3, h4, .item, a.entryLink, .title").firstOrNull() ?: continue
            val title = titleEl.text().trim()
            if (title.isEmpty() || title == "公告" || title == "Announcements" || title == "课程公告") continue

            val detailsEl = container.select(".details, .postedBy, span.date, .metadata, .time").firstOrNull()
            var published = detailsEl?.text()?.trim() ?: ""
            if (published.isEmpty()) {
                val candidate = container.select("p, span, div").firstOrNull { 
                    val t = it.text()
                    t.contains("发布") || t.contains("posted on", ignoreCase = true) || t.contains("Posted by", ignoreCase = true)
                }
                if (candidate != null) {
                    published = candidate.text().trim()
                }
            }

            val bodyEl = container.select(".vtbegenerated, .announcement-body, .description, .content").firstOrNull()
            val content = if (bodyEl != null) {
                cleanHtmlToText(bodyEl)
            } else {
                // Sibling text after titleEl
                var c = ""
                var next = titleEl.nextElementSibling()
                var count = 0
                while (next != null && next.tagName() != "h3" && count < 20) {
                    val text = next.text().trim()
                    if (text.isNotEmpty() && !text.contains("发布") && !text.contains("posted on", ignoreCase = true)) {
                        val formatted = cleanHtmlToText(next)
                        c += if (c.isEmpty()) formatted else "\n$formatted"
                    }
                    next = next.nextElementSibling()
                    count++
                }
                c.ifEmpty { cleanHtmlToText(container).removePrefix(title).trim() }
            }

            val rawID = container.id().ifEmpty { titleEl.id() }
            val id = rawID.ifEmpty { TeachingURLs.digest(title + published) }
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
                        attachments = parseAttachments(container)
                    )
                )
            }
        }

        // Strategy 2: Fallback heading selector (if containers didn't catch anything)
        if (result.isEmpty()) {
            for (heading in doc.select("#announcementList h3, #content_listContainer h3, .announcement h3, .vtbegenerated h3, h3.item")) {
                val title = heading.text().trim()
                if (title.isEmpty() || title == "公告" || title == "Announcements" || title == "课程公告") continue
                val parent = heading.parent() ?: heading
                var content = ""
                var published = parent.select(".details, .postedBy, span.date").text().trim()
                var next = heading.nextElementSibling()
                var count = 0
                while (next != null && next.tagName() != "h3" && count < 20) {
                    val text = next.text().trim()
                    if (text.contains("发布") || text.contains("posted on", ignoreCase = true)) {
                        if (published.isEmpty()) published = text
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
        if (normalized.isEmpty()) return null

        // 1. ISO 8601 (e.g. 2024-09-01T10:00:00.000Z or 2024-09-01T10:00:00Z)
        try {
            val instant = java.time.Instant.parse(normalized)
            return Date.from(instant)
        } catch (_: Exception) {}

        // 2. Chinese pattern with full regex search (e.g. 2024年10月15日 星期二 下午 23:59 or 发布时间: 2024年9月2日 08:30)
        val chineseRegex = Regex("""(\d{4})年(\d{1,2})月(\d{1,2})日(?:\s*(?:星期\S)?)?(?:\s*(上午|下午))?\s*(\d{1,2}):(\d{2})(?::(\d{2}))?""")
        val match = chineseRegex.find(normalized)
        if (match != null) {
            try {
                val year = match.groupValues[1].toInt()
                val month = match.groupValues[2].toInt()
                val day = match.groupValues[3].toInt()
                val ampm = match.groupValues[4]
                var hour = match.groupValues[5].toInt()
                val minute = match.groupValues[6].toInt()
                val second = match.groupValues[7].toIntOrNull() ?: 0
                if (ampm == "下午" && hour < 12) hour += 12
                if (ampm == "上午" && hour == 12) hour = 0
                val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
                cal.set(year, month - 1, day, hour, minute, second)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                return cal.time
            } catch (_: Exception) {}
        }

        // 3. Extract date pattern like 2024-09-01 10:00:00 or 2024/09/01 10:00 from string
        val stdDateRegex = Regex("""(\d{4}[-/]\d{1,2}[-/]\d{1,2}(?:\s+\d{1,2}:\d{2}(?::\d{2})?)?)""")
        val stdMatch = stdDateRegex.find(normalized)
        if (stdMatch != null) {
            val candidate = stdMatch.groupValues[1]
            val formats = listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy/MM/dd HH:mm",
                "yyyy-MM-dd",
                "yyyy/MM/dd"
            )
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
                    return sdf.parse(candidate)
                } catch (_: Exception) {}
            }
        }

        // 4. English patterns
        val enFormats = listOf(
            "EEEE, MMMM d, yyyy h:mm:ss a",
            "EEEE, MMMM d, yyyy h:mm a",
            "MMMM d, yyyy h:mm:ss a",
            "MMMM d, yyyy h:mm a",
            "MMM d, yyyy h:mm a",
            "MMM d, yyyy"
        )
        for (format in enFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
                return sdf.parse(normalized)
            } catch (_: Exception) {}
        }
        return null
    }
}
