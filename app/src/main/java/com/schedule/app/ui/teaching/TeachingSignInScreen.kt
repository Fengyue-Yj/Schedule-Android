package com.schedule.app.ui.teaching

import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.schedule.app.teaching.TeachingURLs

@Composable
fun TeachingSignInScreen(
    onSignInSuccess: () -> Unit
) {
    var hasSignedIn by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val cookies = CookieManager.getInstance().getCookie("https://course.pku.edu.cn")
                        if (cookies != null && cookies.contains("s_session_id") && !hasSignedIn) {
                            hasSignedIn = true
                            onSignInSuccess()
                        }
                    }
                }
                loadUrl(TeachingURLs.login)
            }
        }
    )
}
