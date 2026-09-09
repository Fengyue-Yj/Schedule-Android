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
            val titleText = portlet.select("span.moduleTitle").text()
            val current = titleText.contains("当前") || titleText.contains("Current", ignoreCase = true)
            for (link in portlet.select("ul.courseListing li a")) {
                val href = link.attr("href")
                val regex = Regex("""key=([\d_]+)""")
                var id = regex.find(href)?.groupValues?.get(1)
                if (id == null) {
                    val url = TeachingURLs.resolve(href) ?: continue
                    id = url.substringAfter("course_id=").substringBefore("&")
                }
                if (id.isNotEmpty() && seen.add(id)) {
                    result.add(TeachingCourse(id, link.text(), current))
                }
            }
        }
        if (result.isEmpty() && doc.select("ul.courseListing, #courseMenuPalette_contents").isEmpty()) {
            throw TeachingError.UnexpectedPage
        }
        return result
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
                    content += if (content.isEmpty()) text else "\n$text"
                }
                next = next.nextElementSibling()
                count++
            }
            if (content.isEmpty()) {
                content = parent.select(".vtbegenerated, .details").text()
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

    fun parseAssignments(html: String, course: TeachingCourse): List<TeachingItem> {
        // Simple adaptation, similar to contents but filtering for assignments
        return parseContents(html, course).filter { it.kind == TeachingKind.ASSIGNMENT }
    }

    fun parseMaterials(html: String, course: TeachingCourse): List<TeachingItem> {
        return parseContents(html, course).filter { it.kind == TeachingKind.MATERIAL }
    }

    private fun parseContents(html: String, course: TeachingCourse): List<TeachingItem> {
        if (isLogin(html)) throw TeachingError.LoginRequired
        val doc = Jsoup.parse(html)
        val result = mutableListOf<TeachingItem>()
        
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
            var id = url?.substringAfter("content_id=")?.substringBefore("&") ?: (if (headerID.isEmpty()) rowID else headerID)
            if (id.isEmpty() || title.isEmpty()) continue
            
            val isFolder = alt.contains("文件夹") || alt.contains("folder") || url?.contains("listContent.jsp") == true
            if (isFolder) continue
            
            val assignment = alt.contains("作业") || alt.contains("assignment") || url?.contains("uploadAssignment") == true
            val detail = if (children.size > 2) children[2] else row
            val body = detail.select(".vtbegenerated").text()
            val attachments = parseAttachments(detail).toMutableList()
            if (alt == "文件" || alt == "file") {
                attachments.add(0, TeachingAttachment(title, TeachingURLs.origin + "/webapps/blackboard/execute/content/file?course_id=${course.id}&content_id=$id&mode=view"))
            }
            if (!assignment && attachments.isEmpty() && url?.contains("bbcswebdav") == true) {
                attachments.add(TeachingAttachment(title, url))
            }
            
            result.add(
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
        return result
    }

    private fun parseAttachments(element: Element): List<TeachingAttachment> {
        val attachments = mutableListOf<TeachingAttachment>()
        val seen = mutableSetOf<String>()
        for (a in element.select("ul.attachments a, audio + ul a, a[href*='bbcswebdav']")) {
            val url = TeachingURLs.resolve(a.attr("href")) ?: continue
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
        val formats = listOf("yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "yyyy-MM-dd", "yyyy/MM/dd")
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
