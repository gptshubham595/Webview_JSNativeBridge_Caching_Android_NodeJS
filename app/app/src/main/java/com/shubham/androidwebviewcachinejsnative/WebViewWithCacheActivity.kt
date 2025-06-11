package com.shubham.androidwebviewcachinejsnative

import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class WebViewWithCacheActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    // Map relative paths to asset paths
    private val assetMap = mapOf(
        "assets/style.css" to "static/style.css",
        "assets/script.js" to "static/script.js",
        "assets/dynamic.js" to "static/dynamic.js",
        "assets/logoImage.png" to "static/logoImage.png",
        "assets/heavy_image.png" to "static/heavy_image.png",
        "assets/font.ttf" to "static/font.ttf"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_webview)

        webView = findViewById<WebView>(R.id.webview)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.addJavascriptInterface(WebAppInterfaceImpl(this), "AndroidInterface")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                Log.d("WebView", "Intercepting: $url")

                // Extract the path after the domain
                val uri = request.url
                val path = uri.path?.removePrefix("/static/") ?: return null

                // Check if we have this asset locally
                assetMap[path]?.let { assetPath ->
                    try {
                        val inputStream = assets.open(assetPath)
                        val mimeType = getMimeType(path)
                        Log.d("WebView", "Serving from assets: $assetPath")
                        return WebResourceResponse(mimeType, "UTF-8", inputStream)
                    } catch (e: Exception) {
                        Log.e("WebView", "Error loading asset: $assetPath", e)
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.e("WebView", "Error loading: ${request?.url} - ${error?.description}")
                super.onReceivedError(view, request, error)
            }
        }

        webView.loadUrl("https://pehzyj-ip-167-103-54-204.tunnelmole.net/static/index.html")
    }

    private fun getMimeType(filename: String): String {
        return when {
            filename.endsWith(".js") -> "application/javascript"
            filename.endsWith(".css") -> "text/css"
            filename.endsWith(".png") -> "image/png"
            filename.endsWith(".jpg") || filename.endsWith(".jpeg") -> "image/jpeg"
            filename.endsWith(".ttf") -> "font/ttf"
            filename.endsWith(".html") -> "text/html"
            else -> "text/plain"
        }
    }
}