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
    fun isLogin(html: String): Boolean {
        val lower = html.lowercase()
        return lower.contains("id=\"loginform\"") ||
               lower.contains("name=\"password\"") ||
               lower.contains("id=\"password\"") ||
               lower.contains("id=\"logpass\"") ||
               lower.contains("name=\"logpass\"") ||
               lower.contains("name=\"username\"") ||
               lower.contains("id=\"user_name\"") ||
               lower.contains("iaaa.pku.edu.cn") ||
               lower.contains("oauth.jsp") ||
               lower.contains("统一身份认证") ||
               lower.contains("请重新登录") ||
               lower.contains("会话已过期") ||
               lower.contains("用户登录") ||
               lower.contains("login_wrapper") ||
               lower.contains("/iaaa/resources")
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

        // Safety check: if result is empty AND document lacks Blackboard portal containers
        val containers = doc.select("div.portlet, ul.courseListing, #courseMenuPalette_contents, #global-nav-link, .portletList, #module\\:_1_1")
        if (result.isEmpty() && containers.isEmpty()) {
            throw TeachingError.LoginRequired
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
            
            var initialDueDate: Long? = null
            var initialDueDateText: String? = null
            if (assignment) {
                val fromTitle = extractDeadlineSnippet(title)
                val parsedFromTitle = fromTitle?.let { parseDate(it) }
                if (parsedFromTitle != null) {
                    initialDueDate = parsedFromTitle.time
                    initialDueDateText = fromTitle
                } else {
                    val fromBody = extractDeadlineSnippet(body) ?: extractDeadlineSnippet(row.text())
                    val parsedFromBody = fromBody?.let { parseDate(it) }
                    if (parsedFromBody != null) {
                        initialDueDate = parsedFromBody.time
                        initialDueDateText = fromBody
                    }
                }
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
                    dueDate = initialDueDate,
                    dueDateText = initialDueDateText,
                    sourceURL = if (assignment) TeachingURLs.assignment(course.id, id) else (url ?: TeachingURLs.content(course.id, id)),
                    attachments = attachments
                )
            )
        }
        return ContentPage(items = items, folders = folders)
    }

    fun parseGradesUrl(html: String, courseId: String): String {
        val doc = Jsoup.parse(html)
        val link = doc.select("#courseMenuPalette_contents li a, #courseMenuPalette_div li a, a.courseMenuLink").firstOrNull {
            val text = it.text().trim()
            val href = it.attr("href")
            text.contains("成绩") || text.contains("我的成绩") || text.contains("Grades", ignoreCase = true) || text.contains("My Grades", ignoreCase = true) || href.contains("myGrades.jsp")
        }
        return link?.attr("href")?.let { TeachingURLs.resolve(it) } ?: TeachingURLs.grades(courseId)
    }

    fun parseGrades(html: String, course: TeachingCourse): List<TeachingItem> {
        if (isLogin(html)) throw TeachingError.LoginRequired
        val doc = Jsoup.parse(html)
        val result = mutableListOf<TeachingItem>()
        val seen = mutableSetOf<String>()

        val rows = doc.select("div.sortable_item_row, div.graded_item_row, #grades_wrapper div.row, table#gradesTable tbody tr, div.mygrades-table div.row")
        for (row in rows) {
            val titleEl = row.select("div.cell.gradable, .itemCat, a.itemCat, a.entryLink, td.cell.gradable, h3, h4, .title").firstOrNull() ?: continue
            val title = titleEl.text().trim()
            if (title.isEmpty() || title == "成绩" || title == "我的成绩" || title == "My Grades") continue

            val catEl = row.select("div.itemCat, span.itemCat, .category, span.itemCategory").firstOrNull()
            var category = catEl?.text()?.trim()
            if (category.isNullOrEmpty()) {
                val candidateCat = row.select("div.info, div.type, span.type, span.category").firstOrNull {
                    val t = it.text()
                    t.contains("类别") || t.contains("Type") || t.contains("Category")
                }
                category = candidateCat?.text()?.substringAfter(":")?.trim()
            }

            val gradeEl = row.select("div.cell.grade, span.grade, span.pointsEarned, .grade, td.grade").firstOrNull()
            val pointsPossibleEl = row.select("span.pointsPossible, .pointsPossible, span.outOf, .total").firstOrNull()

            var rawScoreText = gradeEl?.text()?.trim() ?: ""
            var pointsPossible = pointsPossibleEl?.text()?.trim()?.removePrefix("/")?.removePrefix("共")?.removePrefix("out of")?.trim()

            if (rawScoreText.contains("/")) {
                val parts = rawScoreText.split("/")
                rawScoreText = parts[0].trim()
                if (pointsPossible.isNullOrEmpty() && parts.size > 1) {
                    pointsPossible = parts[1].trim()
                }
            }

            val needsGrading = row.select("img[alt*='需要评分'], img[alt*='Needs Grading'], span[title*='需要评分'], span[title*='Needs Grading'], .needsGrading").isNotEmpty()

            val (score, status) = when {
                needsGrading -> Pair("待评分", "待评分")
                rawScoreText.contains("需要评分") || rawScoreText.contains("Needs Grading") -> Pair("待评分", "待评分")
                rawScoreText == "-" || rawScoreText == "--" || rawScoreText.isEmpty() -> Pair(null, "未出分")
                else -> Pair(rawScoreText, "已评分")
            }

            val dateEl = row.select("div.cell.date, div.timestamp, span.timestamp, div.activityDate, div.lastActivity, .date").firstOrNull()
            var dateText = dateEl?.text()?.trim()
            if (dateText.isNullOrEmpty()) {
                val candidateDate = row.select("span, div").firstOrNull {
                    val t = it.text().trim()
                    t.contains("提交时间") || t.contains("活动时间") || t.contains("Submitted") || t.contains("Graded on")
                }
                dateText = candidateDate?.text()?.trim()
            }
            val dateTimestamp = dateText?.let { parseDate(it)?.time }

            val feedbackEl = row.select("div.comment, div.comments, div.feedback, div.evalFeedback, a[title*='反馈'], a[title*='Feedback']").firstOrNull()
            var feedbackText = feedbackEl?.text()?.trim()?.removePrefix("教师评语:")?.removePrefix("Instructor Feedback:")?.trim()
            if (feedbackText.isNullOrEmpty()) {
                val feedbackTitle = feedbackEl?.attr("title")?.takeIf { it.isNotBlank() && !it.contains("查看") }
                feedbackText = feedbackTitle
            }

            val linkHref = titleEl.attr("href").ifBlank { titleEl.select("a").attr("href") }
            val sourceURL = if (linkHref.isNotBlank()) {
                TeachingURLs.resolve(linkHref) ?: TeachingURLs.grades(course.id)
            } else {
                TeachingURLs.grades(course.id)
            }

            val rawID = row.id().ifEmpty { titleEl.id() }
            val itemID = if (rawID.isNotEmpty()) rawID else TeachingURLs.digest(course.id + title + (category ?: ""))

            if (seen.add(itemID)) {
                result.add(
                    TeachingItem(
                        id = "${course.id}:grade:$itemID",
                        courseID = course.id,
                        courseTitle = course.title,
                        contentID = itemID,
                        kind = TeachingKind.GRADE,
                        title = title,
                        body = feedbackText ?: (if (category != null) "类别: $category" else ""),
                        dueDate = null,
                        dueDateText = null,
                        publishedText = dateText,
                        publishedAt = dateTimestamp,
                        sourceURL = sourceURL,
                        attachments = emptyList(),
                        score = score,
                        pointsPossible = pointsPossible,
                        gradeCategory = category,
                        feedback = feedbackText,
                        gradeStatus = status
                    )
                )
            }
        }

        return result
    }

    fun parseApiGrades(columnsJson: String?, usersMeJson: String?, course: TeachingCourse): List<TeachingItem> {
        if (columnsJson.isNullOrBlank() || usersMeJson.isNullOrBlank()) return emptyList()
        val result = mutableListOf<TeachingItem>()
        try {
            val gson = Gson()
            val colObj = gson.fromJson(columnsJson, com.google.gson.JsonObject::class.java)
            val userObj = gson.fromJson(usersMeJson, com.google.gson.JsonObject::class.java)

            val colArray = colObj?.getAsJsonArray("results") ?: return emptyList()
            val userArray = userObj?.getAsJsonArray("results") ?: return emptyList()

            val columnsMap = mutableMapOf<String, com.google.gson.JsonObject>()
            for (colElem in colArray) {
                if (colElem.isJsonObject) {
                    val c = colElem.asJsonObject
                    val cid = c.get("id")?.asString
                    if (!cid.isNullOrEmpty()) {
                        columnsMap[cid] = c
                    }
                }
            }

            for (gradeElem in userArray) {
                if (!gradeElem.isJsonObject) continue
                val g = gradeElem.asJsonObject
                val columnId = g.get("columnId")?.asString ?: continue
                val col = columnsMap[columnId] ?: continue

                val title = col.get("name")?.asString?.trim() ?: continue
                val possibleScore = col.getAsJsonObject("score")?.get("possible")?.asDouble
                val pointsPossible = possibleScore?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }

                val rawScore = g.get("score")?.asDouble
                val scoreText = g.get("text")?.asString?.trim()
                val statusStr = g.get("status")?.asString
                val feedback = g.get("feedback")?.asString?.trim()
                val gradedDateStr = g.get("graded")?.asString
                val gradedTimestamp = gradedDateStr?.let { parseDate(it)?.time }

                val finalScore = when {
                    statusStr.equals("NeedsGrading", ignoreCase = true) -> "待评分"
                    !scoreText.isNullOrEmpty() -> scoreText
                    rawScore != null -> if (rawScore % 1.0 == 0.0) rawScore.toInt().toString() else rawScore.toString()
                    else -> null
                }

                val finalStatus = when {
                    statusStr.equals("NeedsGrading", ignoreCase = true) -> "待评分"
                    finalScore != null -> "已评分"
                    else -> "未出分"
                }

                result.add(
                    TeachingItem(
                        id = "${course.id}:grade:$columnId",
                        courseID = course.id,
                        courseTitle = course.title,
                        contentID = columnId,
                        kind = TeachingKind.GRADE,
                        title = title,
                        body = feedback ?: "",
                        publishedText = gradedDateStr,
                        publishedAt = gradedTimestamp,
                        sourceURL = TeachingURLs.grades(course.id),
                        score = finalScore,
                        pointsPossible = pointsPossible,
                        gradeCategory = null,
                        feedback = feedback,
                        gradeStatus = finalStatus
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun extractDeadlineSnippet(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val clean = text.replace("\u00a0", " ").trim()

        // 1. Keyword prefix with date:
        // Keywords: 截止时间, 截止日期, 截止, 到期时间, 到期日, 到期, 提交时间, 提交截止, 提交期限, Due Date, Due, DDL, ddl, Ddl
        val prefixRegex = Regex(
            """(?:截止时间|截止日期|截止|到期时间|到期日|到期|提交时间|提交截止|提交期限|Due\s*Date|Due|DDL|ddl|Ddl)[:：\s]+([0-9]{1,4}[年\-/\.月][^\n\r<，,；;]{1,35})"""
        )
        prefixRegex.find(clean)?.let {
            return it.groupValues[1].trim()
        }

        // 2. Date followed by keyword (e.g. "10月20日 23:59 截止", "10月20日23:59前提交", "2024-10-20 23:59截止")
        val suffixRegex = Regex(
            """([0-9]{1,4}[年\-/\.月][^\n\r<，,；;]{1,35}?)\s*(?:前(?:提交|上传)|截止|到期)"""
        )
        suffixRegex.find(clean)?.let {
            return it.groupValues[1].trim()
        }

        // 3. Parentheses containing deadline (e.g. "(10月20日 23:59截止)", "(DDL: 10/20 23:59)")
        val parenRegex = Regex(
            """[（\(](?:DDL|ddl|截止|到期)?[:：\s]*([0-9]{1,4}[年\-/\.月][^\)）\n\r<]{1,35}?)(?:前(?:提交|上传)|截止|到期)?[）\)]"""
        )
        parenRegex.find(clean)?.let {
            return it.groupValues[1].trim()
        }

        // 4. Chinese full date pattern directly
        val chineseFullRegex = Regex(
            """(\d{4}年\d{1,2}月\d{1,2}日(?:\s*(?:[(（]?星期\S[)）]?)?)?(?:\s*(?:上午|下午|晚上|中午|早上))?\s*\d{1,2}[:：点时]\d{2}(?:[:：分秒]\d{2})?|\d{4}年\d{1,2}月\d{1,2}日)"""
        )
        chineseFullRegex.find(clean)?.let {
            return it.groupValues[1].trim()
        }

        // 5. Standard full date pattern directly (e.g. 2024-10-20 23:59)
        val stdFullRegex = Regex(
            """(\d{4}[-/\.]\d{1,2}[-/\.]\d{1,2}(?:\s+\d{1,2}[:：]\d{2}(?:[:：]\d{2})?)?)"""
        )
        stdFullRegex.find(clean)?.let {
            return it.groupValues[1].trim()
        }

        // 6. Chinese month-day with time directly (e.g. 10月20日 23:59)
        val chineseMonthDayRegex = Regex(
            """(\d{1,2}月\d{1,2}日(?:\s*(?:[(（]?星期\S[)）]?)?)?(?:\s*(?:上午|下午|晚上|中午|早上))?\s*\d{1,2}[:：点时]\d{2}(?:[:：分秒]\d{2})?)"""
        )
        chineseMonthDayRegex.find(clean)?.let {
            return it.groupValues[1].trim()
        }

        return null
    }

    fun parseDeadline(html: String): Pair<Long?, String?> {
        if (isLogin(html)) throw TeachingError.LoginRequired
        val doc = Jsoup.parse(html)

        // 1. Blackboard classic metadata selectors
        val metaSelectors = listOf(
            "div[id^='assignMeta']:matches(到期|截止|Due|due) + div",
            ".steplabel:matches(到期|截止|Due|due) + .stepcontent",
            "tr:has(th:matches(到期|截止|Due|due)) td",
            "tr:has(td:matches(到期|截止|Due|due)) td:not(:matches(到期|截止|Due|due))",
            "#assignMeta2 + div",
            "#assignMeta1 + div",
            "#assignMeta + div",
            "div[id^='assignMeta'] + div",
            "span.activityDate",
            "#dueDate",
            "span#dueDate",
            "div.dueDate",
            "span.dueDate"
        )
        for (sel in metaSelectors) {
            val elements = try { doc.select(sel) } catch (_: Exception) { null } ?: continue
            for (el in elements) {
                val text = el.text().trim()
                if (text.isNotEmpty()) {
                    val d = parseDate(text)
                    if (d != null) {
                        return Pair(d.time, text)
                    }
                    val snippet = extractDeadlineSnippet(text)
                    if (snippet != null) {
                        val sd = parseDate(snippet)
                        if (sd != null) {
                            return Pair(sd.time, snippet)
                        }
                    }
                }
            }
        }

        // 2. Blackboard table rows / definition lists / p / div with labels
        val labelSelectors = listOf(
            "tr:contains(截止日期)",
            "tr:contains(截止时间)",
            "tr:contains(到期时间)",
            "tr:contains(到期日)",
            "tr:contains(到期)",
            "tr:contains(Due Date)",
            "tr:contains(Due)",
            "div.metadataItem",
            "div:contains(截止时间)",
            "div:contains(截止日期)",
            "div:contains(到期日)",
            "li:contains(截止日期)",
            "li:contains(截止时间)",
            "p:contains(截止日期)",
            "p:contains(截止时间)"
        )
        for (sel in labelSelectors) {
            val elements = try { doc.select(sel) } catch (_: Exception) { null } ?: continue
            for (el in elements) {
                val raw = el.text().trim()
                val snippet = extractDeadlineSnippet(raw)
                if (snippet != null) {
                    val parsed = parseDate(snippet)
                    if (parsed != null) {
                        return Pair(parsed.time, snippet)
                    }
                }
            }
        }

        // 3. Assignment instructions or main content div
        for (contentSel in listOf("#instructionText", ".vtbegenerated", "#assignmentInstructions", ".details", ".contentList")) {
            val contentEl = doc.select(contentSel).firstOrNull()
            if (contentEl != null) {
                val snippet = extractDeadlineSnippet(contentEl.text())
                if (snippet != null) {
                    val parsed = parseDate(snippet)
                    if (parsed != null) {
                        return Pair(parsed.time, snippet)
                    }
                }
            }
        }

        // 4. Fallback: Search the entire body text
        val bodyText = doc.body().text()
        val snippet = extractDeadlineSnippet(bodyText)
        if (snippet != null) {
            val parsed = parseDate(snippet)
            if (parsed != null) {
                return Pair(parsed.time, snippet)
            }
        }

        return Pair(null, null)
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

        fun inferYear(month: Int): Int {
            val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
            val curYear = cal.get(java.util.Calendar.YEAR)
            val curMonth = cal.get(java.util.Calendar.MONTH) + 1
            return when {
                curMonth >= 9 && month <= 2 -> curYear + 1 // Fall semester crossing into new year
                curMonth <= 2 && month >= 9 -> curYear - 1
                else -> curYear
            }
        }

        fun buildDate(year: Int, month: Int, day: Int, hourStr: String?, minStr: String?, secStr: String?, ampm: String?): Date? {
            var hour = hourStr?.toIntOrNull() ?: 23
            var minute = minStr?.toIntOrNull() ?: 59
            var second = secStr?.toIntOrNull() ?: 0

            // Handle 24:00 (frequent in Chinese assignment deadlines -> clamp to 23:59:59 of same day)
            if (hour >= 24) {
                hour = 23
                minute = 59
                second = 59
            } else {
                if ((ampm == "下午" || ampm == "晚上") && hour < 12) hour += 12
                if (ampm == "上午" && hour == 12) hour = 0
            }

            return try {
                val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
                cal.set(year, month - 1, day, hour, minute, second)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.time
            } catch (_: Exception) {
                null
            }
        }

        // 2. Chinese pattern with full 4-digit year: 2024年10月15日 [星期二] [下午] 23:59[:00] or 23点59分 or 23点
        val chineseFull = Regex(
            """(\d{4})年(\d{1,2})月(\d{1,2})日(?:\s*(?:[(（]?星期\S[)）]?)?)?(?:\s*(上午|下午|晚上|中午|早上))?(?:\s*(\d{1,2})[:：点时](\d{2})(?:[:：分秒](\d{2}))?|\s*(\d{1,2})点)?"""
        )
        chineseFull.find(normalized)?.let { m ->
            val year = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            val day = m.groupValues[3].toInt()
            val ampm = m.groupValues[4].takeIf { it.isNotEmpty() }
            val hour = m.groupValues[5].ifEmpty { m.groupValues[8] }.takeIf { it.isNotEmpty() }
            val minute = m.groupValues[6].takeIf { it.isNotEmpty() }
            val second = m.groupValues[7].takeIf { it.isNotEmpty() }
            val d = buildDate(year, month, day, hour, minute, second, ampm)
            if (d != null) return d
        }

        // 3. Chinese pattern with month-day (no year): 10月15日 [星期二] [下午] 23:59[:00] or 23点59分 or 23点
        val chineseMonthDay = Regex(
            """(\d{1,2})月(\d{1,2})日(?:\s*(?:[(（]?星期\S[)）]?)?)?(?:\s*(上午|下午|晚上|中午|早上))?(?:\s*(\d{1,2})[:：点时](\d{2})(?:[:：分秒](\d{2}))?|\s*(\d{1,2})点)?"""
        )
        chineseMonthDay.find(normalized)?.let { m ->
            val month = m.groupValues[1].toInt()
            val day = m.groupValues[2].toInt()
            if (month in 1..12 && day in 1..31) {
                val year = inferYear(month)
                val ampm = m.groupValues[3].takeIf { it.isNotEmpty() }
                val hour = m.groupValues[4].ifEmpty { m.groupValues[7] }.takeIf { it.isNotEmpty() }
                val minute = m.groupValues[5].takeIf { it.isNotEmpty() }
                val second = m.groupValues[6].takeIf { it.isNotEmpty() }
                val d = buildDate(year, month, day, hour, minute, second, ampm)
                if (d != null) return d
            }
        }

        // 4. Standard full date: 2024-10-15 23:59:00 or 2024/10/15 23:59 or 2024.10.15 23:59
        val stdFull = Regex(
            """(\d{4})[-/\.](\d{1,2})[-/\.](\d{1,2})(?:\s+(\d{1,2})[:：](\d{2})(?:[:：](\d{2}))?)?"""
        )
        stdFull.find(normalized)?.let { m ->
            val year = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            val day = m.groupValues[3].toInt()
            val hour = m.groupValues[4].takeIf { it.isNotEmpty() }
            val minute = m.groupValues[5].takeIf { it.isNotEmpty() }
            val second = m.groupValues[6].takeIf { it.isNotEmpty() }
            val d = buildDate(year, month, day, hour, minute, second, null)
            if (d != null) return d
        }

        // 5. Standard month-day with time: 10-15 23:59 or 10/15 23:59 or 10.15 23:59
        val stdMonthDay = Regex(
            """(?:^|[^\d])(\d{1,2})[-/\.](\d{1,2})\s+(\d{1,2})[:：](\d{2})(?:[:：](\d{2}))?"""
        )
        stdMonthDay.find(normalized)?.let { m ->
            val month = m.groupValues[1].toInt()
            val day = m.groupValues[2].toInt()
            if (month in 1..12 && day in 1..31) {
                val year = inferYear(month)
                val hour = m.groupValues[3]
                val minute = m.groupValues[4]
                val second = m.groupValues[5].takeIf { it.isNotEmpty() }
                val d = buildDate(year, month, day, hour, minute, second, null)
                if (d != null) return d
            }
        }

        // 6. English patterns
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
