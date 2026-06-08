package com.example.limbmotionrecoveryapp.screens.profile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.screens.home.LearnTutorialActivity
import com.example.limbmotionrecoveryapp.screens.home.LearnTutorials
import com.example.limbmotionrecoveryapp.screens.home.TutorialBookmarks

class SavedVideosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_videos)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        loadSaved()
    }

    private fun loadSaved() {
        val container = findViewById<LinearLayout>(R.id.savedContainer)
        val empty = findViewById<View>(R.id.emptyState)
        val scroll = findViewById<View>(R.id.scrollContent)
        val tvCount = findViewById<TextView>(R.id.tvCount)

        val savedTitles = TutorialBookmarks.getAll(this)
        val saved = LearnTutorials.all.filter { it.title in savedTitles }

        if (saved.isEmpty()) {
            tvCount.text = ""
            empty.visibility = View.VISIBLE
            scroll.visibility = View.GONE
            return
        }

        tvCount.text = "${saved.size} saved"
        empty.visibility = View.GONE
        scroll.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(this)
        val density = resources.displayMetrics.density

        saved.forEachIndexed { index, tutorial ->
            if (index > 0) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1 * density).toInt()
                    ).also { it.marginStart = (108 * density).toInt() }
                    setBackgroundColor(Color.parseColor("#F0F2F1"))
                }
                container.addView(divider)
            }

            val item = inflater.inflate(R.layout.item_saved_tutorial, container, false)

            item.findViewById<View>(R.id.savedThumbBg)
                .setBackgroundColor(Color.parseColor(tutorial.thumbColor))
            val thumbImg = item.findViewById<ImageView>(R.id.savedThumbImg)
            if (tutorial.youtubeId.isNotBlank()) {
                thumbImg.visibility = View.VISIBLE
                Glide.with(this)
                    .load("https://img.youtube.com/vi/${tutorial.youtubeId}/hqdefault.jpg")
                    .centerCrop()
                    .into(thumbImg)
            }
            item.findViewById<TextView>(R.id.savedTitle).text = tutorial.title
            val meta = listOfNotNull(
                tutorial.author.ifBlank { null },
                tutorial.videoDuration.ifBlank { tutorial.duration }
            ).joinToString(" · ")
            item.findViewById<TextView>(R.id.savedMeta).text = meta
            item.findViewById<TextView>(R.id.savedCategory).text = tutorial.category

            item.setOnClickListener {
                startActivity(Intent(this, LearnTutorialActivity::class.java).apply {
                    putExtra(LearnTutorialActivity.EXTRA_TITLE, tutorial.title)
                    putExtra(LearnTutorialActivity.EXTRA_DESCRIPTION, tutorial.description)
                    putExtra(LearnTutorialActivity.EXTRA_DURATION, tutorial.duration)
                    putExtra(LearnTutorialActivity.EXTRA_YOUTUBE_ID, tutorial.youtubeId)
                    putExtra(LearnTutorialActivity.EXTRA_CATEGORY, tutorial.category)
                    putExtra(LearnTutorialActivity.EXTRA_AUTHOR, tutorial.author)
                    putExtra(LearnTutorialActivity.EXTRA_VIDEO_DURATION, tutorial.videoDuration)
                })
            }
            container.addView(item)
        }
    }
}
