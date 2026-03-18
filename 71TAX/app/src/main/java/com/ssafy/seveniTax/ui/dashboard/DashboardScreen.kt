package com.ssafy.seveniTax.ui.dashboard

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.seveniTax.bridge.WebBridge
import com.ssafy.seveniTax.ui.navigation.Route
import com.ssafy.seveniTax.util.Constants
import com.ssafy.seveniTax.viewmodel.DashboardViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var webView: WebView? by remember { mutableStateOf(null) }

    BackHandler {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        }
        // 대시보드에서는 뒤로가기 막음 (홈)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    setSupportZoom(false)
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean = false
                }

                webChromeClient = WebChromeClient()

                addJavascriptInterface(
                    WebBridge(
                        onNavigate = { route ->
                            // JS 브리지는 백그라운드 스레드 → 메인 스레드로 전환
                            mainHandler.post {
                                when (route) {
                                    "card_list"   -> navController.navigate(Route.CardList.path)
                                    "card_change" -> navController.navigate(Route.CardChange.path)
                                    "pay_intro"   -> navController.navigate(Route.PayIntro.path)
                                    else          -> navController.navigate(route)
                                }
                            }
                        },
                        onRequestBiometric = { callbackId ->
                            // TODO: 생체인증 후 JS 콜백
                        },
                        onRequestToken = { viewModel.getAccessToken() }
                    ),
                    "AndroidBridge"
                )

                loadUrl(Constants.WEBVIEW_BASE_URL)
                webView = this
            }
        }
    )
}
