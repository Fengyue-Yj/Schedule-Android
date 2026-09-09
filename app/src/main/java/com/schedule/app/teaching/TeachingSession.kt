package com.schedule.app.teaching

import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.InputStreamReader

class TeachingSession {
    private val cookieManager = CookieManager.getInstance()

    suspend fun get(urlString: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15000
        connection.readTimeout = 25000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 Schedule/1.0")
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.5")
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        
        val cookies = cookieManager.getCookie(urlString) ?: cookieManager.getCookie("https://course.pku.edu.cn")
        if (cookies != null) {
            connection.setRequestProperty("Cookie", cookies)
        }
        
        try {
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                InputStreamReader(connection.inputStream, "UTF-8").use { it.readText() }
            } else {
                throw TeachingError.NetworkError("HTTP $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    fun isLoggedIn(): Boolean {
        val cookies = cookieManager.getCookie("https://course.pku.edu.cn") ?: return false
        return cookies.isNotBlank() && (
            cookies.contains("session", ignoreCase = true) ||
            cookies.contains("JSESSIONID", ignoreCase = true) ||
            cookies.contains("s_session", ignoreCase = true) ||
            cookies.contains("pku", ignoreCase = true)
        )
    }

    fun getUserID(): String? {
        val cookies = cookieManager.getCookie("https://course.pku.edu.cn") ?: return null
        return cookies.hashCode().toString()
    }
}
