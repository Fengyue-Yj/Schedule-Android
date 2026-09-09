package com.schedule.app.ui.teaching

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.schedule.app.teaching.TeachingURLs
import com.schedule.app.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TeachingSignInScreen(
    onNavigateBack: () -> Unit,
    onSignInSuccess: () -> Unit
) {
    var hasSignedIn by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("北大教学网登录") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        CookieManager.getInstance().flush()
                        val cookies = CookieManager.getInstance().getCookie("https://course.pku.edu.cn")
                        if (!cookies.isNullOrBlank() && !hasSignedIn) {
                            hasSignedIn = true
                            onSignInSuccess()
                        }
                    }) {
                        Text("连接", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "请在北大统一身份认证系统页面输入账号密码。应用仅在本地保存登录会话，不会记录您的密码。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webViewRef = this
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 Schedule/1.0"
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url ?: return false
                                val urlStr = url.toString()
                                // PKU's registered OAuth callback still names HTTP; upgrade to HTTPS before loading
                                if (url.scheme?.equals("http", ignoreCase = true) == true) {
                                    val host = url.host?.lowercase() ?: ""
                                    if (host == "pku.edu.cn" || host.endsWith(".pku.edu.cn")) {
                                        val secureUrl = urlStr.replaceFirst("http://", "https://")
                                        view?.loadUrl(secureUrl)
                                        return true
                                    }
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url != null) {
                                    currentUrl = url
                                    CookieManager.getInstance().flush()

                                    // If reached Blackboard portal page, login is complete
                                    val isPortal = url.contains("course.pku.edu.cn") &&
                                            (url.contains("/portal/") || url.contains("tabAction") || url.contains("/webapps/portal"))
                                    if (isPortal && !hasSignedIn) {
                                        val cookies = CookieManager.getInstance().getCookie("https://course.pku.edu.cn")
                                        if (!cookies.isNullOrBlank()) {
                                            hasSignedIn = true
                                            onSignInSuccess()
                                        }
                                    }
                                }
                            }
                        }

                        loadUrl(TeachingURLs.login)
                    }
                }
            )
        }
    }
}

