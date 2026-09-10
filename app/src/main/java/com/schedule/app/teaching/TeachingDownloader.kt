package com.schedule.app.teaching

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import android.media.MediaScannerConnection
import java.nio.charset.StandardCharsets

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Float, val readBytes: Long, val totalBytes: Long) : DownloadStatus()
    data class Success(val file: File, val message: String = "") : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

object TeachingDownloader {

    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    fun getStatus(url: String): DownloadStatus {
        return _downloadStates.value[url] ?: DownloadStatus.Idle
    }

    private fun updateStatus(url: String, status: DownloadStatus) {
        val current = _downloadStates.value.toMutableMap()
        current[url] = status
        _downloadStates.value = current
    }

    /**
     * Check if a file with given name already exists in the app's download directory
     */
    fun getDownloadedFile(context: Context, suggestedFileName: String): File? {
        val sanitized = sanitizeFileName(suggestedFileName)
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val file = File(dir, sanitized)
        if (file.exists() && file.length() > 0) {
            // Check if file is actually a corrupt HTML error page from previous download
            try {
                if (file.length() < 20000) {
                    val header = file.inputStream().use {
                        val buf = ByteArray(minOf(file.length().toInt(), 4096))
                        val read = it.read(buf)
                        if (read > 0) String(buf, 0, read, StandardCharsets.UTF_8) else ""
                    }
                    if (header.contains("<html", ignoreCase = true) && 
                        (header.contains("无访问权限") || header.contains("没有权限") || header.contains("拒绝访问") || header.contains("loginForm") || header.contains("iaaa.pku.edu.cn"))
                    ) {
                        file.delete()
                        return null
                    }
                }
            } catch (_: Exception) {}
            return file
        }
        return null
    }

    fun isDownloaded(context: Context, suggestedFileName: String): Boolean {
        return getDownloadedFile(context, suggestedFileName) != null
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

    /**
     * Download attachment using session cookies from CookieManager
     */
    suspend fun download(
        context: Context,
        url: String,
        suggestedFileName: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        updateStatus(url, DownloadStatus.Downloading(0f, 0L, -1L))
        try {
            var currentUrl = url
            var previousUrl = "https://course.pku.edu.cn/"
            var connection: HttpURLConnection? = null
            var redirectCount = 0
            val maxRedirects = 8

            val cookieManager = CookieManager.getInstance()

            while (redirectCount < maxRedirects) {
                // Ensure HTTPS for PKU domains
                if (currentUrl.startsWith("http://course.pku.edu.cn") || currentUrl.startsWith("http://iaaa.pku.edu.cn")) {
                    currentUrl = currentUrl.replaceFirst("http://", "https://")
                }

                val parsedUrl = URL(currentUrl)
                connection = parsedUrl.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 Schedule/1.0"
                )
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.5")
                connection.setRequestProperty("Referer", previousUrl)

                // Inject all PKU cookies with deduplicated merge
                val specificCookies = cookieManager.getCookie(currentUrl) ?: ""
                val originCookies = cookieManager.getCookie(TeachingURLs.origin) ?: ""
                val webappsCookies = cookieManager.getCookie("${TeachingURLs.origin}/webapps") ?: ""
                val allCookies = mergeCookies(originCookies, webappsCookies, specificCookies)
                if (allCookies.isNotEmpty()) {
                    connection.setRequestProperty("Cookie", allCookies)
                }

                connection.connect()

                // Save any Set-Cookie headers
                val setCookies = connection.headerFields["Set-Cookie"] ?: connection.headerFields["set-cookie"] ?: emptyList()
                for (header in setCookies) {
                    cookieManager.setCookie(currentUrl, header)
                    cookieManager.setCookie("https://course.pku.edu.cn", header)
                }
                if (setCookies.isNotEmpty()) {
                    cookieManager.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (location != null) {
                        previousUrl = currentUrl
                        currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                            location
                        } else {
                            URL(URL(currentUrl), location).toString()
                        }
                        connection.disconnect()
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val conn = connection ?: throw IllegalStateException("Failed to connect")
            if (conn.responseCode !in 200..299) {
                val errorMsg = "HTTP ${conn.responseCode}: ${conn.responseMessage}"
                updateStatus(url, DownloadStatus.Error(errorMsg))
                return@withContext Result.failure(Exception(errorMsg))
            }

            // Determine final filename
            val contentDisposition = conn.getHeaderField("Content-Disposition")
            val contentType = conn.contentType
            var finalName = extractFileName(contentDisposition, suggestedFileName, currentUrl)

            // Ensure valid extension
            finalName = ensureFileExtension(finalName, contentType)
            finalName = sanitizeFileName(finalName)

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!dir.exists()) dir.mkdirs()

            val targetFile = File(dir, finalName)
            val totalBytes = conn.contentLengthLong

            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var readBytes = 0L
                    var bytes: Int
                    var firstChunk = true

                    while (input.read(buffer).also { bytes = it } != -1) {
                        if (firstChunk) {
                            firstChunk = false
                            val sample = String(buffer, 0, minOf(bytes, 4096), StandardCharsets.UTF_8)
                            if (sample.contains("id=\"loginForm\"") || sample.contains("name=\"password\"") || sample.contains("iaaa.pku.edu.cn")) {
                                targetFile.delete()
                                updateStatus(url, DownloadStatus.Error("登录已过期，请重新登录教学网"))
                                return@withContext Result.failure(TeachingError.LoginRequired)
                            }
                            if (sample.contains("无访问权限") || sample.contains("没有权限") || sample.contains("拒绝访问") || sample.contains("Access Denied") || sample.contains("receiptBad")) {
                                targetFile.delete()
                                val errorMsg = "教学网提示无访问权限，请在网页确认资料权限"
                                updateStatus(url, DownloadStatus.Error(errorMsg))
                                return@withContext Result.failure(Exception(errorMsg))
                            }
                            val expectedDoc = suggestedFileName?.let { name ->
                                val ext = name.substringAfterLast('.', "").lowercase()
                                ext in listOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "zip", "rar", "7z", "mp4", "mp3")
                            } ?: false
                            if (expectedDoc && sample.contains("<html", ignoreCase = true)) {
                                targetFile.delete()
                                val errorMsg = "教学网返回了HTML网页而非课件文件，可能暂无下载权限"
                                updateStatus(url, DownloadStatus.Error(errorMsg))
                                return@withContext Result.failure(Exception(errorMsg))
                            }
                        }

                        output.write(buffer, 0, bytes)
                        readBytes += bytes

                        val progress = if (totalBytes > 0) {
                            (readBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                        } else {
                            -1f
                        }
                        updateStatus(url, DownloadStatus.Downloading(progress, readBytes, totalBytes))
                    }
                    output.flush()
                }
            }

            // Also copy to public Downloads folder so it appears in phone's Files app
            try {
                saveToPublicDownloads(context, targetFile, finalName, contentType)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            updateStatus(url, DownloadStatus.Success(targetFile, "已下载至: ${targetFile.name}"))
            Result.success(targetFile)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "下载失败"
            updateStatus(url, DownloadStatus.Error(errorMsg))
            Result.failure(e)
        }
    }

    /**
     * Copy downloaded file into Android system public Downloads via MediaStore or public dir
     */
    private fun saveToPublicDownloads(
        context: Context,
        srcFile: File,
        displayName: String,
        mimeType: String?
    ) {
        val resolvedMime = mimeType ?: getMimeType(srcFile)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, resolvedMime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Schedule")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    srcFile.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Schedule")
            if (!publicDir.exists()) publicDir.mkdirs()
            val dest = File(publicDir, displayName)
            srcFile.copyTo(dest, overwrite = true)
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(resolvedMime), null)
        }
    }

    fun openFile(context: Context, file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun shareFile(context: Context, file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = getMimeType(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "分享文件: ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getMimeType(file: File): String {
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    private fun extractFileName(contentDisposition: String?, suggestedName: String?, url: String): String {
        if (!contentDisposition.isNullOrBlank()) {
            // Check RFC 5987 filename*=UTF-8''...
            val utf8Match = Regex("filename\\*\\s*=\\s*UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(contentDisposition)
            if (utf8Match != null) {
                try {
                    return URLDecoder.decode(utf8Match.groupValues[1].trim(), StandardCharsets.UTF_8.name())
                } catch (_: Exception) {}
            }

            // Check standard filename="..."
            val normalMatch = Regex("filename\\s*=\\s*\"?([^;\"]+)\"?", RegexOption.IGNORE_CASE).find(contentDisposition)
            if (normalMatch != null) {
                val raw = normalMatch.groupValues[1].trim()
                return try {
                    String(raw.toByteArray(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
                } catch (_: Exception) {
                    raw
                }
            }
        }

        if (!suggestedName.isNullOrBlank()) {
            return suggestedName.trim()
        }

        return try {
            val path = URL(url).path
            val name = path.substringAfterLast("/")
            if (name.isNotBlank()) URLDecoder.decode(name, StandardCharsets.UTF_8.name()) else "material_file"
        } catch (_: Exception) {
            "material_file"
        }
    }

    private fun ensureFileExtension(fileName: String, contentType: String?): String {
        if (fileName.contains(".") && fileName.substringAfterLast(".").length in 2..5) {
            return fileName
        }
        if (contentType != null) {
            val cleanType = contentType.substringBefore(";").trim().lowercase()
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(cleanType)
            if (!ext.isNullOrBlank()) {
                return "$fileName.$ext"
            }
        }
        return fileName
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }
}
