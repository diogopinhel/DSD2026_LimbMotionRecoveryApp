package com.example.limbmotionrecoveryapp.screens.home

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
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
        val author = intent.getStringExtra(EXTRA_AUTHOR) ?: ""
        val videoDuration = intent.getStringExtra(EXTRA_VIDEO_DURATION) ?: ""

        // Toolbar
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvToolbarTitle).text = title

        val btnBookmark = findViewById<ImageButton>(R.id.btnBookmark)
        isBookmarked = TutorialBookmarks.isSaved(this, title)
        applyBookmarkState(btnBookmark)
        btnBookmark.setOnClickListener { toggleBookmark(btnBookmark, title) }

        val shareAction = { shareArticle(title) }
        findViewById<View>(R.id.btnShareToolbar).setOnClickListener { shareAction() }
        findViewById<View>(R.id.btnShareAction).setOnClickListener { shareAction() }

        // Content
        findViewById<TextView>(R.id.tvTutorialTitle).text = title
        findViewById<TextView>(R.id.tvTutorialDescription).text = description
        findViewById<TextView>(R.id.tvDurationPill).text = "$duration read"
        findViewById<TextView>(R.id.tvCategoryPill).text = category

        val authorRow = findViewById<LinearLayout>(R.id.authorRow)
        val tvAuthor = findViewById<TextView>(R.id.tvAuthor)
        if (author.isNotBlank()) {
            tvAuthor.text = author
            authorRow.visibility = View.VISIBLE
        }

        // Player labels
        val durationBadge = videoDuration.ifBlank { duration.replace(" min", ":00") }
        findViewById<TextView>(R.id.tvPlayerCategory).text = category
        findViewById<TextView>(R.id.tvPlayerDuration).text = durationBadge

        // Player setup
        val btnPlay = findViewById<View>(R.id.btnPlay)
        val ivThumbnail = findViewById<ImageView>(R.id.ivThumbnail)
        val tvComingSoon = findViewById<TextView>(R.id.tvComingSoon)

        if (youtubeId.isNotBlank()) {
            ivThumbnail.visibility = View.VISIBLE
            Glide.with(this)
                .load("https://img.youtube.com/vi/$youtubeId/hqdefault.jpg")
                .centerCrop()
                .into(ivThumbnail)
        }

        findViewById<FrameLayout>(R.id.playerContainer).setOnClickListener {
            if (youtubeId.isNotBlank()) {
                openYouTube(youtubeId)
            } else {
                if (videoPlayed) return@setOnClickListener
                videoPlayed = true
                btnPlay.animate().alpha(0f).setDuration(180).withEndAction {
                    btnPlay.visibility = View.GONE
                }.start()
                tvComingSoon.alpha = 0f
                tvComingSoon.visibility = View.VISIBLE
                tvComingSoon.animate().alpha(1f).setDuration(300).start()
            }
        }

        setupRelatedArticles(title)
    }

    private fun toggleBookmark(btn: ImageButton, title: String) {
        isBookmarked = !isBookmarked
        if (isBookmarked) TutorialBookmarks.save(this, title)
        else TutorialBookmarks.remove(this, title)
        applyBookmarkState(btn)
    }

    private fun applyBookmarkState(btn: ImageButton) {
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

    private fun openYouTube(videoId: String) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
            setPackage("com.google.android.youtube")
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        startActivity(
            if (packageManager.resolveActivity(appIntent, 0) != null) appIntent else webIntent
        )
    }

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
                    putExtra(EXTRA_AUTHOR, tutorial.author)
                    putExtra(EXTRA_VIDEO_DURATION, tutorial.videoDuration)
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
        const val EXTRA_AUTHOR = "extra_author"
        const val EXTRA_VIDEO_DURATION = "extra_video_duration"
    }
}
