package com.example.limbmotionrecoveryapp.screens.home

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R

class LearnTutorialActivity : AppCompatActivity() {

    private var isBookmarked = false
    private var videoPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn_tutorial)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val description = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""
        val duration = intent.getStringExtra(EXTRA_DURATION) ?: ""
        val youtubeId = intent.getStringExtra(EXTRA_YOUTUBE_ID) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Recovery basics"

        // Toolbar
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvToolbarTitle).text = title

        val btnBookmark = findViewById<ImageButton>(R.id.btnBookmark)
        btnBookmark.setOnClickListener { toggleBookmark(btnBookmark) }

        val shareAction = { shareArticle(title) }
        findViewById<View>(R.id.btnShareToolbar).setOnClickListener { shareAction() }
        findViewById<View>(R.id.btnShareAction).setOnClickListener { shareAction() }

        findViewById<View>(R.id.btnStartExercises).setOnClickListener { finish() }

        // Content
        findViewById<TextView>(R.id.tvTutorialTitle).text = title
        findViewById<TextView>(R.id.tvTutorialDescription).text = description
        findViewById<TextView>(R.id.tvDurationPill).text = "$duration read"
        findViewById<TextView>(R.id.tvCategoryPill).text = category

        // Player labels
        val durationBadge = duration.replace(" min", ":00")
        findViewById<TextView>(R.id.tvPlayerCategory).text = category
        findViewById<TextView>(R.id.tvPlayerDuration).text = durationBadge

        // Player setup
        val webView = findViewById<WebView>(R.id.webViewYoutube)
        val btnPlay = findViewById<View>(R.id.btnPlay)
        val tvComingSoon = findViewById<TextView>(R.id.tvComingSoon)

        setupWebView(webView)

        findViewById<FrameLayout>(R.id.playerContainer).setOnClickListener {
            if (videoPlayed) return@setOnClickListener
            videoPlayed = true

            btnPlay.animate().alpha(0f).setDuration(180).withEndAction {
                btnPlay.visibility = View.GONE
            }.start()

            if (youtubeId.isNotBlank()) {
                webView.visibility = View.VISIBLE
                webView.loadDataWithBaseURL(
                    "https://www.youtube.com",
                    buildYouTubeHtml(youtubeId),
                    "text/html", "utf-8", null
                )
            } else {
                tvComingSoon.alpha = 0f
                tvComingSoon.visibility = View.VISIBLE
                tvComingSoon.animate().alpha(1f).setDuration(300).start()
            }
        }

        setupRelatedArticles(title)
    }

    private fun toggleBookmark(btn: ImageButton) {
        isBookmarked = !isBookmarked
        if (isBookmarked) {
            btn.setBackgroundResource(R.drawable.bg_circle_btn_active)
            btn.setColorFilter(getColor(R.color.colorPrimaryGreen))
        } else {
            btn.setBackgroundResource(R.drawable.bg_circle_btn)
            btn.setColorFilter(getColor(R.color.colorTextMedium))
        }
    }

    private fun shareArticle(title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this recovery article: $title")
        }
        startActivity(Intent.createChooser(intent, null))
    }

    private fun setupWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun onVideoEnded() = runOnUiThread { showDone() }
        }, "Android")
    }

    private fun buildYouTubeHtml(videoId: String) = """
        <!DOCTYPE html><html>
        <head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>html,body{margin:0;padding:0;background:#000;height:100%;}
        #player{position:absolute;top:0;left:0;width:100%;height:100%;}</style>
        </head>
        <body>
        <div id="player"></div>
        <script>
          var tag=document.createElement('script');
          tag.src='https://www.youtube.com/iframe_api';
          document.head.appendChild(tag);
          var player;
          function onYouTubeIframeAPIReady(){
            player=new YT.Player('player',{
              videoId:'$videoId',
              playerVars:{playsinline:1,rel:0,autoplay:1},
              events:{onStateChange:function(e){if(e.data===0)Android.onVideoEnded();}}
            });
          }
        </script></body></html>
    """.trimIndent()

    private fun showDone() {
        val progressFill = findViewById<View>(R.id.progressFill)
        val doneDivider = findViewById<View>(R.id.doneDivider)
        val doneRow = findViewById<View>(R.id.doneRow)

        progressFill.post {
            val targetWidth = (progressFill.parent as FrameLayout).width
            val anim = ValueAnimator.ofInt(progressFill.width, targetWidth)
            anim.duration = 600
            anim.addUpdateListener {
                val lp = progressFill.layoutParams
                lp.width = it.animatedValue as Int
                progressFill.layoutParams = lp
            }
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    doneDivider.visibility = View.VISIBLE
                    doneRow.alpha = 0f
                    doneRow.visibility = View.VISIBLE
                    doneRow.animate().alpha(1f).setDuration(300).start()
                }
            })
            anim.start()
        }
    }

    private fun setupRelatedArticles(currentTitle: String) {
        val container = findViewById<LinearLayout>(R.id.relatedContainer)
        val all = LearnTutorials.all
        val currentIdx = all.indexOfFirst { it.title == currentTitle }.takeIf { it >= 0 } ?: 0

        // Icon backgrounds and matching tint colours, cycling through 3 variants
        val iconBgRes = listOf(R.drawable.bg_icon_blue, R.drawable.bg_icon_amber, R.drawable.bg_icon_green)
        val iconTints = listOf(
            Color.parseColor("#185FA5"),
            Color.parseColor("#854F0B"),
            Color.parseColor("#0F6E56")
        )

        val gap = (8 * resources.displayMetrics.density).toInt()
        val inflater = LayoutInflater.from(this)

        for (offset in 1..2) {
            val idx = (currentIdx + offset) % all.size
            val tutorial = all[idx]
            val colorIdx = idx % iconBgRes.size

            val card = inflater.inflate(R.layout.item_tutorial_related, container, false)
            if (offset > 1) {
                (card.layoutParams as LinearLayout.LayoutParams).topMargin = gap
            }

            card.findViewById<FrameLayout>(R.id.relIconBg)
                .setBackgroundResource(iconBgRes[colorIdx])
            card.findViewById<ImageView>(R.id.relIcon)
                .setColorFilter(iconTints[colorIdx])
            card.findViewById<TextView>(R.id.relTitle).text = tutorial.title
            card.findViewById<TextView>(R.id.relDuration).text = "${tutorial.duration} read"

            card.setOnClickListener {
                startActivity(Intent(this, LearnTutorialActivity::class.java).apply {
                    putExtra(EXTRA_TITLE, tutorial.title)
                    putExtra(EXTRA_DESCRIPTION, tutorial.description)
                    putExtra(EXTRA_DURATION, tutorial.duration)
                    putExtra(EXTRA_YOUTUBE_ID, tutorial.youtubeId)
                    putExtra(EXTRA_CATEGORY, tutorial.category)
                })
            }
            container.addView(card)
        }
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_YOUTUBE_ID = "extra_youtube_id"
        const val EXTRA_CATEGORY = "extra_category"
    }
}
