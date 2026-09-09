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
        val cookies = cookieManager.getCookie(urlString)
        if (cookies != null) {
            connection.setRequestProperty("Cookie", cookies)
        }
        
        try {
            val responseCode = connection.responseCode
            if (responseCode == 200) {
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
        return cookies.contains("s_session_id")
    }

    fun getUserID(): String? {
        val cookies = cookieManager.getCookie("https://course.pku.edu.cn") ?: return null
        // Extract a session identifier or account ID from cookies if needed
        // For simplicity, just return a hashed version of cookies
        return cookies.hashCode().toString()
    }
}
