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
        return if (file.exists() && file.length() > 0) file else null
    }

    fun isDownloaded(context: Context, suggestedFileName: String): Boolean {
        return getDownloadedFile(context, suggestedFileName) != null
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
            var connection: HttpURLConnection? = null
            var redirectCount = 0
            val maxRedirects = 6

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
                connection.setRequestProperty("Referer", "https://course.pku.edu.cn/")

                // Inject PKU cookies
                val specificCookies = cookieManager.getCookie(currentUrl) ?: ""
                val originCookies = cookieManager.getCookie(TeachingURLs.origin) ?: ""
                val allCookies = if (specificCookies.isNotBlank() && originCookies.isNotBlank() && specificCookies != originCookies) {
                    "$originCookies; $specificCookies"
                } else {
                    specificCookies.ifBlank { originCookies }
                }
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
                            if (contentType?.contains("text/html") == true) {
                                val sample = String(buffer, 0, minOf(bytes, 2048), StandardCharsets.UTF_8)
                                if (sample.contains("id=\"loginForm\"") || sample.contains("name=\"password\"") || sample.contains("iaaa.pku.edu.cn")) {
                                    updateStatus(url, DownloadStatus.Error("登录已过期，请重新登录教学网"))
                                    return@withContext Result.failure(TeachingError.LoginRequired)
                                }
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
