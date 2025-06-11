package com.shubham.androidwebviewcachinejsnative

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.net.URLEncoder

class WebViewWithoutCacheActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webview)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
//        webView.addJavascriptInterface(WebAppInterfaceImpl(this), "OlaElectricJSNativeBridge")
        webView.addJavascriptInterface(WebAppInterfaceImpl(this), "AndroidInterface")

        webView.webViewClient = WebViewClient() // No shouldInterceptRequest
        webView.loadUrl("https://pehzyj-ip-167-103-54-204.tunnelmole.net/static/index.html")

//        val postData = "oem_auth_token=" + URLEncoder.encode("eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyX3R5cGUiOiJDT05TVU1FUiIsInRyYWZmaWNfYnVja2V0IjoyOTYsImFwcF9tb2RlIjoiRElTQ09WRVJZIiwidGVuYW50X2lkIjoiNDY0NDQ2YTYtODZkNC00NGY1LTlhZWUtMmUyMDljM2NiOGEyIiwidGVuYW50X3R5cGUiOiJUV09fV0hFRUxFUiIsImVudGl0eV90eXBlIjoiRlVSWSIsImRldmljZV9pZCI6IjE0NTk3ODg2YzA4NGIxMDEyNTVkMjE3OGE3MTUzMTNlIiwidG9rZW5fdHlwZSI6ImFjY2Vzc190b2tlbiIsImV4cGlyZV9hdCI6MTc0OTcxMzU1MTkzMiwiaWF0IjoxNzQ5NjI3MTUxLCJleHAiOjE3NDk3MTM1NTF9.d0kpuzBgeOgbKqrsNaW6hpcuQHoMXNu5JNclS2g2OVo", "UTF-8")
//        webView.postUrl("http://172.20.84.159:3000/toaddContact", postData.toByteArray())
    }
}