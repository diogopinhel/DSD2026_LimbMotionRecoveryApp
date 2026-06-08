package com.example.limbmotionrecoveryapp.screens.home

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class PainCheckInActivity : AppCompatActivity() {

    data class PainEntry(val level: Int, val notes: String?, val timestamp: Long)

    private val prefs by lazy { getSharedPreferences("pain_prefs", Context.MODE_PRIVATE) }
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing) updateCountdown()
            handler.postDelayed(this, 60_000)
        }
    }

    private var selectedLevel: Int? = null
    private val allButtons = mutableListOf<TextView>()

    private lateinit var layoutRegister: LinearLayout
    private lateinit var layoutLocked: LinearLayout
    private lateinit var tvSelectedLabel: TextView
    private lateinit var tvLockedInfo: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var etNotes: EditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmptyHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pain_check_in)

        layoutRegister = findViewById(R.id.layoutRegister)
        layoutLocked = findViewById(R.id.layoutLocked)
        tvSelectedLabel = findViewById(R.id.tvSelectedLabel)
        tvLockedInfo = findViewById(R.id.tvLockedInfo)
        tvCountdown = findViewById(R.id.tvCountdown)
        etNotes = findViewById(R.id.etNotes)
        btnSubmit = findViewById(R.id.btnSubmit)
        rvHistory = findViewById(R.id.rvHistory)
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory)

        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        buildPainButtons()
        btnSubmit.setOnClickListener { onSubmit() }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (isLockedToday()) handler.post(countdownRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(countdownRunnable)
    }

    private fun buildPainButtons() {
        val row1 = findViewById<LinearLayout>(R.id.painRow1)
        val row2 = findViewById<LinearLayout>(R.id.painRow2)
        for (i in 1..5) row1.addView(createButton(i))
        for (i in 6..10) row2.addView(createButton(i))
    }

    private fun createButton(level: Int): TextView {
        val tv = TextView(this)
        val size = dpToPx(48)
        val params = LinearLayout.LayoutParams(size, size).apply {
            setMargins(dpToPx(4), 0, dpToPx(4), 0)
        }
        tv.layoutParams = params
        tv.text = level.toString()
        tv.textSize = 15f
        tv.setTypeface(null, Typeface.BOLD)
        tv.gravity = Gravity.CENTER
        tv.setTextColor(Color.WHITE)
        tv.background = circleDrawable(colorForLevel(level), 0.35f)
        tv.setOnClickListener { selectLevel(level) }
        allButtons.add(tv)
        return tv
    }

    private fun selectLevel(level: Int) {
        selectedLevel = level
        allButtons.forEachIndexed { idx, tv ->
            val l = idx + 1
            tv.background = if (l == level)
                circleDrawable(colorForLevel(l), 1f)
            else
                circleDrawable(colorForLevel(l), 0.35f)
        }
        tvSelectedLabel.text = "Level $level — ${labelForLevel(level)}"
        tvSelectedLabel.setTextColor(Color.parseColor(colorForLevel(level)))
        btnSubmit.isEnabled = true
    }

    private fun onSubmit() {
        val level = selectedLevel ?: return
        val notes = etNotes.text.toString().trim().ifBlank { null }
        saveEntry(PainEntry(level, notes, System.currentTimeMillis()))
        refresh()
    }

    private fun refresh() {
        val history = loadEntries()
        renderHistory(history)
        if (isLockedToday()) {
            layoutRegister.visibility = View.GONE
            layoutLocked.visibility = View.VISIBLE
            val last = history.firstOrNull()
            if (last != null) {
                tvLockedInfo.text = "You logged level ${last.level} — ${labelForLevel(last.level)}"
            }
            handler.post(countdownRunnable)
        } else {
            layoutRegister.visibility = View.VISIBLE
            layoutLocked.visibility = View.GONE
            handler.removeCallbacks(countdownRunnable)
        }
    }

    private fun updateCountdown() {
        val ms = msUntilMidnight()
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        tvCountdown.text = "Next check-in in ${h}h ${m}m"
    }

    private fun renderHistory(entries: List<PainEntry>) {
        if (entries.isEmpty()) {
            rvHistory.visibility = View.GONE
            tvEmptyHistory.visibility = View.VISIBLE
            return
        }
        rvHistory.visibility = View.VISIBLE
        tvEmptyHistory.visibility = View.GONE
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = HistoryAdapter(entries)
    }

    private fun saveEntry(entry: PainEntry) {
        val list = loadEntries().toMutableList()
        list.add(0, entry)
        prefs.edit().putString("entries", gson.toJson(list)).apply()
    }

    private fun loadEntries(): List<PainEntry> {
        val json = prefs.getString("entries", null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<PainEntry>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    private fun isLockedToday(): Boolean {
        val entries = loadEntries()
        if (entries.isEmpty()) return false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(entries.first().timestamp)) == sdf.format(Date())
    }

    private fun msUntilMidnight(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis - System.currentTimeMillis()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun circleDrawable(hexColor: String, alpha: Float): GradientDrawable {
        val gd = GradientDrawable()
        gd.shape = GradientDrawable.OVAL
        val c = Color.parseColor(hexColor)
        gd.setColor(Color.argb((alpha * 255).toInt(), Color.red(c), Color.green(c), Color.blue(c)))
        return gd
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

    private inner class HistoryAdapter(private val items: List<PainEntry>) :
        RecyclerView.Adapter<HistoryAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvLevel: TextView = v.findViewById(R.id.tvPainLevel)
            val tvLabel: TextView = v.findViewById(R.id.tvPainLabel)
            val tvDate: TextView = v.findViewById(R.id.tvPainDate)
            val tvNotes: TextView = v.findViewById(R.id.tvPainNotes)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pain_log, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = items[position]
            holder.tvLevel.text = e.level.toString()
            holder.tvLevel.setTextColor(Color.parseColor(colorForLevel(e.level)))
            holder.tvLabel.text = labelForLevel(e.level)
            holder.tvDate.text = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                .format(Date(e.timestamp))
            if (!e.notes.isNullOrBlank()) {
                holder.tvNotes.visibility = View.VISIBLE
                holder.tvNotes.text = e.notes
            } else {
                holder.tvNotes.visibility = View.GONE
            }
        }
    }
}
