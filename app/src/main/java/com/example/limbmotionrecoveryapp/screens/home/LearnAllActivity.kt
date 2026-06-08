package com.example.limbmotionrecoveryapp.screens.home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R

class LearnAllActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learn_all)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val grid = findViewById<LinearLayout>(R.id.learnAllGrid)
        val inflater = LayoutInflater.from(this)
        val gap = (8 * resources.displayMetrics.density).toInt()
        val tutorials = LearnTutorials.all

        // Build rows of 2 cards each
        tutorials.chunked(2).forEach { row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = gap }
            }

            row.forEachIndexed { col, tutorial ->
                val cardView = inflater.inflate(R.layout.item_learn_card_large, rowLayout, false)
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (col > 0) lp.marginStart = gap
                cardView.layoutParams = lp

                cardView.findViewById<View>(R.id.learnThumbBg)
                    .setBackgroundColor(Color.parseColor(tutorial.thumbColor))
                cardView.findViewById<TextView>(R.id.learnTitle).text = tutorial.title
                cardView.findViewById<TextView>(R.id.learnDuration).text = tutorial.duration

                cardView.setOnClickListener {
                    startActivity(Intent(this, LearnTutorialActivity::class.java).apply {
                        putExtra(LearnTutorialActivity.EXTRA_TITLE, tutorial.title)
                        putExtra(LearnTutorialActivity.EXTRA_DESCRIPTION, tutorial.description)
                        putExtra(LearnTutorialActivity.EXTRA_DURATION, tutorial.duration)
                        putExtra(LearnTutorialActivity.EXTRA_YOUTUBE_ID, tutorial.youtubeId)
                        putExtra(LearnTutorialActivity.EXTRA_CATEGORY, tutorial.category)
                    })
                }
                rowLayout.addView(cardView)
            }

            // If odd number of tutorials, fill last slot with invisible placeholder
            if (row.size == 1) {
                val placeholder = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = gap }
                }
                rowLayout.addView(placeholder)
            }

            grid.addView(rowLayout)
        }
    }
}
