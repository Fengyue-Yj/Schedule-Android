package com.schedule.app.teaching

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class TeachingSession {
    private val cookieManager = CookieManager.getInstance()

    suspend fun get(urlString: String): String = withContext(Dispatchers.IO) {
        var currentUrl = urlString
        var redirectCount = 0
        val maxRedirects = 8
        var finalHtml: String? = null

        while (redirectCount < maxRedirects) {
            // Ensure HTTPS for PKU domains
            if (currentUrl.startsWith("http://course.pku.edu.cn") || currentUrl.startsWith("http://iaaa.pku.edu.cn")) {
                currentUrl = currentUrl.replaceFirst("http://", "https://")
            }

            val url = URL(currentUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false // Handle redirects manually to support cross-protocol and cookie preservation
            connection.connectTimeout = 15000
            connection.readTimeout = 25000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 Schedule/1.0"
            )
            connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.5")
            connection.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            )
            connection.setRequestProperty("Referer", "https://course.pku.edu.cn/")

            // Collect all available PKU cookies across all paths
            val mergedCookies = TeachingDownloader.getAllPkuCookies(cookieManager, currentUrl, "https://course.pku.edu.cn/")
            if (mergedCookies.isNotBlank()) {
                connection.setRequestProperty("Cookie", mergedCookies)
            }

            try {
                connection.connect()
                val responseCode = connection.responseCode

                // Persist any Set-Cookie headers
                saveCookiesFromHeaders(currentUrl, connection)

                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        val resolved = if (location.startsWith("http://") || location.startsWith("https://")) {
                            location
                        } else {
                            URL(URL(currentUrl), location).toString()
                        }
                        currentUrl = resolved
                        redirectCount++
                        continue
                    }
                }

                if (responseCode in 200..299) {
                    val charset = extractCharset(connection.contentType) ?: "UTF-8"
                    finalHtml = InputStreamReader(connection.inputStream, charset).use { it.readText() }
                    break
                } else {
                    throw TeachingError.NetworkError("HTTP $responseCode: ${connection.responseMessage}")
                }
            } finally {
                connection.disconnect()
            }
        }

        val html = finalHtml ?: throw TeachingError.NetworkError("Failed to load page (Too many redirects)")

        // Check if redirected to login page
        if (html.contains("id=\"loginForm\"") || html.contains("name=\"password\"") || html.contains("iaaa.pku.edu.cn/iaaa/oauth.jsp")) {
            throw TeachingError.LoginRequired
        }

        html
    }

    suspend fun getOrNull(urlString: String): String? = try {
        get(urlString)
    } catch (e: Exception) {
        null
    }

    private fun saveCookiesFromHeaders(url: String, connection: HttpURLConnection) {
        val headerFields = connection.headerFields
        val setCookieHeaders = headerFields["Set-Cookie"] ?: headerFields["set-cookie"] ?: emptyList()
        for (header in setCookieHeaders) {
            cookieManager.setCookie(url, header)
            cookieManager.setCookie("https://course.pku.edu.cn", header)
        }
        if (setCookieHeaders.isNotEmpty()) {
            cookieManager.flush()
        }
    }

    private fun mergeCookies(vararg cookieStrings: String): String {
        val map = mutableMapOf<String, String>()
        for (str in cookieStrings) {
            str.split(";").forEach { part ->
                val trimmed = part.trim()
                if (trimmed.contains("=")) {
                    val k = trimmed.substringBefore("=").trim()
                    val v = trimmed.substringAfter("=").trim()
                    if (k.isNotEmpty() && v.isNotEmpty()) {
                        map[k] = v
                    }
                }
            }
        }
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun extractCharset(contentType: String?): String? {
        if (contentType == null) return null
        val match = Regex("""charset=([a-zA-Z0-9_-]+)""").find(contentType)
        return match?.groupValues?.get(1)
    }

    fun isLoggedIn(): Boolean {
        val cookies = cookieManager.getCookie("https://course.pku.edu.cn") ?: ""
        return cookies.isNotBlank() && (
            cookies.contains("session", ignoreCase = true) ||
            cookies.contains("JSESSIONID", ignoreCase = true) ||
            cookies.contains("s_session", ignoreCase = true) ||
            cookies.contains("pku", ignoreCase = true) ||
            cookies.length > 15
        )
    }

    fun getUserID(): String? {
        val cookies = cookieManager.getCookie("https://course.pku.edu.cn") ?: return null
        return cookies.hashCode().toString()
    }
}
