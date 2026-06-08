package com.example.limbmotionrecoveryapp.screens.profile

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.screens.home.PainCheckInActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class PainLogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pain_log)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        loadEntries()
    }

    private fun loadEntries() {
        val empty = findViewById<View>(R.id.emptyState)
        val scroll = findViewById<View>(R.id.scrollContent)
        val list = findViewById<LinearLayout>(R.id.painList)
        val tvCount = findViewById<TextView>(R.id.tvEntryCount)

        val entries = readEntries()

        if (entries.isEmpty()) {
            tvCount.text = ""
            empty.visibility = View.VISIBLE
            scroll.visibility = View.GONE
            return
        }

        tvCount.text = "${entries.size} entries"
        empty.visibility = View.GONE
        scroll.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(this)
        val dateFmt = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())

        entries.forEach { entry ->
            val item = inflater.inflate(R.layout.item_pain_log, list, false)

            item.findViewById<TextView>(R.id.tvPainLevel).apply {
                text = entry.level.toString()
                setTextColor(Color.parseColor(colorForLevel(entry.level)))
            }
            item.findViewById<TextView>(R.id.tvPainLabel).text = labelForLevel(entry.level)
            item.findViewById<TextView>(R.id.tvPainDate).text = dateFmt.format(Date(entry.timestamp))

            val tvNotes = item.findViewById<TextView>(R.id.tvPainNotes)
            if (!entry.notes.isNullOrBlank()) {
                tvNotes.visibility = View.VISIBLE
                tvNotes.text = entry.notes
            } else {
                tvNotes.visibility = View.GONE
            }

            list.addView(item)
        }
    }

    private fun readEntries(): List<PainCheckInActivity.PainEntry> {
        val json = getSharedPreferences("pain_prefs", Context.MODE_PRIVATE)
            .getString("entries", null) ?: return emptyList()
        return try {
            Gson().fromJson(json, object : TypeToken<List<PainCheckInActivity.PainEntry>>() {}.type)
        } catch (_: Exception) { emptyList() }
    }

    private fun colorForLevel(level: Int): String = when {
        level <= 3 -> "#1D9E75"
        level <= 6 -> "#BA7517"
        else -> "#E24B4A"
    }

    private fun labelForLevel(level: Int): String = when (level) {
        1 -> "No pain"
        2, 3 -> "Minimal"
        4, 5 -> "Mild"
        6, 7 -> "Moderate"
        8, 9 -> "Severe"
        10 -> "Worst possible"
        else -> ""
    }
}
