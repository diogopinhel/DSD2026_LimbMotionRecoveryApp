package com.example.limbmotionrecoveryapp.screens.home

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R

class LearnTutorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn_tutorial)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        val duration = intent.getStringExtra(EXTRA_DURATION) ?: ""
        val youtubeId = intent.getStringExtra(EXTRA_YOUTUBE_ID) ?: ""

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvToolbarTitle).text = title
        findViewById<TextView>(R.id.tvTutorialTitle).text = title
        findViewById<TextView>(R.id.tvTutorialDescription).text = description
        findViewById<TextView>(R.id.tvTutorialDuration).text = duration

        val webView = findViewById<WebView>(R.id.webViewYoutube)
        webView.settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()

        if (youtubeId.isNotBlank()) {
            val embedHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                <style>
                  body { margin:0; padding:0; background:#000; }
                  iframe { width:100%; height:100%; border:0; }
                </style>
                </head>
                <body>
                <iframe src="https://www.youtube.com/embed/$youtubeId?playsinline=1&rel=0"
                  allowfullscreen allow="autoplay; encrypted-media">
                </iframe>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "utf-8", null)
        } else {
            webView.loadDataWithBaseURL(null,
                "<html><body style='background:#111;display:flex;align-items:center;justify-content:center;height:100%;margin:0;'>" +
                "<p style='color:#888;font-family:sans-serif;font-size:14px;text-align:center;'>Video coming soon</p>" +
                "</body></html>",
                "text/html", "utf-8", null)
        }
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_YOUTUBE_ID = "extra_youtube_id"
    }
}
