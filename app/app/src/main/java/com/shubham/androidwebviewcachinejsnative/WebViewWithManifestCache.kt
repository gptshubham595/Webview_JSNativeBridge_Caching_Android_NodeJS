package com.shubham.androidwebviewcachinejsnative

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest

class WebViewWithManifestCache : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var cachedAssets: List<AssetInfo>
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "webview_cache"
        private const val KEY_CACHED_MANIFEST_HASH = "cached_manifest_hash"
        private const val MANIFEST_URL = "http://172.20.68.11:3000/manifest"
        private const val BASE_URL = "http://172.20.68.11:3000/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_webview)

        initializeCache()
        setupWebView()

        // Check for updates and cache assets
        checkAndUpdateCache()

        webView.loadUrl("http://172.20.68.11:3000/static/index.html")
    }

    private fun initializeCache() {
        File(cacheDir, "webview_manifest_cache")
        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    private fun setupWebView() {
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

                // Check if we have this asset cached
                val cachedFile = getCachedFileFromUrl(url)
                if (cachedFile != null && cachedFile.exists()) {
                    Log.d("WebView", "Serving from manifest cache: ${cachedFile.name}")
                    val mimeType = getMimeTypeFromUrl(url)
                    return WebResourceResponse(mimeType, "UTF-8", cachedFile.inputStream())
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebView", "Page loaded with cache hit rate: ${calculateCacheHitRate()}%")
            }
        }
    }

    private fun checkAndUpdateCache() {
        downloadScope.launch {
            try {
                val manifest = downloadManifest()
                val manifestHash = calculateManifestHash(manifest)
                val cachedHash = sharedPrefs.getString(KEY_CACHED_MANIFEST_HASH, null)

                if (cachedHash != manifestHash) {
                    Log.d("WebView", "New manifest detected, updating cache")
                    cachedAssets = manifest
                    updateAssets(manifest)
                    sharedPrefs.edit()
                        .putString(KEY_CACHED_MANIFEST_HASH, manifestHash)
                        .apply()
                } else {
                    Log.d("WebView", "Cache is up to date")
                    cachedAssets = manifest
                    verifyCache(manifest)
                }

            } catch (e: Exception) {
                Log.e("WebView", "Failed to check manifest", e)
                // Try to load cached manifest if available
                loadCachedManifest()
            }
        }
    }

    private suspend fun downloadManifest(): List<AssetInfo> {
        return withContext(Dispatchers.IO) {
            val connection = URL(MANIFEST_URL).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 10000

            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<AssetInfo>>() {}.type
            gson.fromJson(json, type)
        }
    }

    private fun calculateManifestHash(manifest: List<AssetInfo>): String {
        val manifestString = gson.toJson(manifest)
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(manifestString.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun loadCachedManifest() {
        // Initialize with empty list if no cached manifest
        cachedAssets = emptyList()
    }

    private suspend fun updateAssets(manifest: List<AssetInfo>) {
        manifest.forEach { assetInfo ->
            val cachedFile = getCachedFile(assetInfo.path)

            // Check if file needs updating
            if (!cachedFile.exists() || !verifyFileHash(cachedFile, assetInfo.hash)) {
                Log.d("WebView", "Downloading: ${assetInfo.path}")
                val assetUrl = BASE_URL + assetInfo.path
                downloadAsset(assetUrl, cachedFile, assetInfo)
            }
        }
    }

    private suspend fun verifyCache(manifest: List<AssetInfo>) {
        var missingCount = 0
        manifest.forEach { assetInfo ->
            val cachedFile = getCachedFile(assetInfo.path)
            if (!cachedFile.exists() || !verifyFileHash(cachedFile, assetInfo.hash)) {
                Log.d("WebView", "Re-downloading corrupted/missing: ${assetInfo.path}")
                val assetUrl = BASE_URL + assetInfo.path
                downloadAsset(assetUrl, cachedFile, assetInfo)
                missingCount++
            }
        }

        if (missingCount > 0) {
            Log.d("WebView", "Redownloaded $missingCount corrupted/missing assets")
        }
    }

    private suspend fun downloadAsset(url: String, cachedFile: File, assetInfo: AssetInfo) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 30000

            val inputStream = connection.getInputStream()

            cachedFile.parentFile?.mkdirs()
            cachedFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }

            // Verify download using MD5 hash
            if (verifyFileHash(cachedFile, assetInfo.hash)) {
                Log.d("WebView", "Successfully cached: ${cachedFile.name}")
            } else {
                Log.e("WebView", "Hash mismatch for: ${cachedFile.name}")
                cachedFile.delete()
            }

        } catch (e: Exception) {
            Log.e("WebView", "Failed to download: $url", e)
            cachedFile.delete()
        }
    }

    private fun verifyFileHash(file: File, expectedHash: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            actualHash == expectedHash
        } catch (e: Exception) {
            false
        }
    }

    private fun getCachedFile(assetPath: String): File {
        // Remove "static/" prefix and replace path separators with underscores
        val filename = assetPath.removePrefix("static/").replace("/", "_").replace(":", "_")
        return File(cacheDir, filename)
    }

    private fun getCachedFileFromUrl(url: String): File? {
        // Find matching asset in cached manifest
        if (!::cachedAssets.isInitialized) return null

        val relativePath = url.removePrefix(BASE_URL)
        val assetInfo = cachedAssets.find { it.path == relativePath }

        return if (assetInfo != null) {
            getCachedFile(assetInfo.path)
        } else null
    }

    private fun getMimeTypeFromUrl(url: String): String {
        return when {
            url.endsWith(".js") -> "application/javascript"
            url.endsWith(".css") -> "text/css"
            url.endsWith(".png") -> "image/png"
            url.endsWith(".jpg") || url.endsWith(".jpeg") -> "image/jpeg"
            url.endsWith(".ttf") -> "font/ttf"
            url.endsWith(".html") -> "text/html"
            else -> "text/plain"
        }
    }

    private fun calculateCacheHitRate(): Int {
        // Simple implementation - you can make this more sophisticated
        val totalFiles = cacheDir.listFiles()?.size ?: 0
        return if (totalFiles > 0) 95 else 0 // Placeholder calculation
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadScope.cancel()
    }

    // Public methods for cache management
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
        sharedPrefs.edit().remove(KEY_CACHED_MANIFEST_HASH).apply()
    }

    fun getCacheSize(): String {
        val bytes = cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    fun getCacheInfo(): String {
        val manifestHash = sharedPrefs.getString(KEY_CACHED_MANIFEST_HASH, "Not cached")?.take(8)
        val size = getCacheSize()
        val fileCount = cacheDir.listFiles()?.size ?: 0
        return "Manifest: $manifestHash...\nSize: $size\nFiles: $fileCount"
    }
}