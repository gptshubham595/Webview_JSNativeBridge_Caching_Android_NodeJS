package com.shubham.androidwebviewcachinejsnative

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_with_cache).setOnClickListener {
            startActivity(Intent(this, WebViewWithCacheActivity::class.java))
        }

        findViewById<Button>(R.id.btn_without_cache).setOnClickListener {
            startActivity(Intent(this, WebViewWithoutCacheActivity::class.java))
        }

        findViewById<Button>(R.id.btn_with_auto_download_cache).setOnClickListener {
            startActivity(Intent(this, WebViewWithAutoDownloadCache::class.java))
        }

        findViewById<Button>(R.id.btn_with_manifest_cache).setOnClickListener {
            startActivity(Intent(this, WebViewWithManifestCache::class.java))
        }
    }
}