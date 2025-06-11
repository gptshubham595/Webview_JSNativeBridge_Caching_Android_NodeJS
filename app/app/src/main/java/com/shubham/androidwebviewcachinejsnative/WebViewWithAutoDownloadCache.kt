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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.net.URL

class WebViewWithAutoDownloadCache : AppCompatActivity() {
    private lateinit var webView: WebView
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Assets that should be cached locally
    private val cacheableAssets = setOf(
        "assets/style.css",
        "assets/script.js",
        "assets/dynamic.js",
        "assets/logoImage.png",
        "assets/heavy_image.png",
        "assets/font.ttf"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_webview)

        File(cacheDir, "webview_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        webView = findViewById<WebView>(R.id.webview)
        setupWebView()

        // Start downloading assets in background
        preloadAssets()

        webView.loadUrl("https://pehzyj-ip-167-103-54-204.tunnelmole.net/static/index.html")
    }

    private fun setupWebView() {
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

                // Check if this is a cacheable asset
                val path = extractAssetPath(url)
                if (path != null && cacheableAssets.contains(path)) {
                    // Try to serve from cache first
                    val cachedFile = getCachedFile(path)
                    if (cachedFile.exists()) {
                        Log.d("WebView", "Serving from cache: $path")
                        return createResponseFromFile(cachedFile, path)
                    }

                    // If not cached, download and cache it
                    Log.d("WebView", "Downloading and caching: $path")
                    return downloadAndCache(url, path)
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
    }

    private fun preloadAssets() {
        downloadScope.launch {
            cacheableAssets.forEach { assetPath ->
                val url = "https://pehzyj-ip-167-103-54-204.tunnelmole.net/static/$assetPath"
                val cachedFile = getCachedFile(assetPath)

                if (!cachedFile.exists()) {
                    Log.d("WebView", "Preloading: $assetPath")
                    downloadAsset(url, cachedFile)
                }
            }
        }
    }

    private fun downloadAndCache(url: String, assetPath: String): WebResourceResponse? {
        return try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 10000

            val inputStream = connection.getInputStream()
            val cachedFile = getCachedFile(assetPath)

            // Save to cache
            cachedFile.parentFile?.mkdirs()
            cachedFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            Log.d("WebView", "Cached: $assetPath")

            // Return response from cached file
            createResponseFromFile(cachedFile, assetPath)

        } catch (e: Exception) {
            Log.e("WebView", "Failed to download: $url", e)
            null
        }
    }

    private suspend fun downloadAsset(url: String, cachedFile: File) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 10000

            val inputStream = connection.getInputStream()

            cachedFile.parentFile?.mkdirs()
            cachedFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            Log.d("WebView", "Preloaded: ${cachedFile.name}")

        } catch (e: Exception) {
            Log.e("WebView", "Failed to preload: $url", e)
        }
    }

    private fun createResponseFromFile(file: File, assetPath: String): WebResourceResponse {
        val mimeType = getMimeType(assetPath)
        return WebResourceResponse(mimeType, "UTF-8", file.inputStream())
    }

    private fun extractAssetPath(url: String): String? {
        return try {
            val uri = URI(url)
            val path = uri.path?.removePrefix("/static/")
            if (path != null && cacheableAssets.contains(path)) path else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getCachedFile(assetPath: String): File {
        val filename = assetPath.replace("/", "_")
        return File(cacheDir, filename)
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

    override fun onDestroy() {
        super.onDestroy()
        downloadScope.cancel()
    }

    // Method to clear cache
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    // Method to get cache size
    fun getCacheSize(): Long {
        return cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
}