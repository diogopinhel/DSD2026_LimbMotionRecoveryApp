package com.example.limbmotionrecoveryapp.screens.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dsd.m1.api.V2ApiClient
import com.example.limbmotionrecoveryapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MyRecoveryActivity : AppCompatActivity() {

    private val api = V2ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_recovery)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val userId = prefs.getInt("userId", 0)

        loadSessions(userId, token)
    }

    private fun loadSessions(userId: Int, token: String) {
        val spinner = findViewById<ProgressBar>(R.id.progressSpinner)
        val scroll = findViewById<View>(R.id.scrollContent)
        val empty = findViewById<View>(R.id.emptyState)
        val list = findViewById<LinearLayout>(R.id.sessionList)

        spinner.visibility = View.VISIBLE

        lifecycleScope.launch {
            val sessions = try {
                withContext(Dispatchers.IO) { api.getSessions(userId, token) }
            } catch (_: Exception) { emptyList() }

            spinner.visibility = View.GONE

            if (sessions.isEmpty()) {
                empty.visibility = View.VISIBLE
                return@launch
            }

            scroll.visibility = View.VISIBLE
            val inflater = LayoutInflater.from(this@MyRecoveryActivity)
            val density = resources.displayMetrics.density

            sessions.forEachIndexed { index, session ->
                if (index > 0) {
                    val divider = View(this@MyRecoveryActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (1 * density).toInt()
                        ).also { it.marginStart = (58 * density).toInt() }
                        setBackgroundColor(0xFFF0F2F1.toInt())
                    }
                    list.addView(divider)
                }

                val item = inflater.inflate(R.layout.item_session, list, false)
                val ts = parseTimestamp(session)

                item.findViewById<TextView>(R.id.tvDay).text =
                    if (ts != null) SimpleDateFormat("dd", Locale.getDefault()).format(Date(ts)) else "—"
                item.findViewById<TextView>(R.id.tvMonth).text =
                    if (ts != null) SimpleDateFormat("MMM", Locale.getDefault()).format(Date(ts)) else ""
                item.findViewById<TextView>(R.id.tvTime).text =
                    if (ts != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts)) else ""

                val payloadStatus = session["payload_status"]?.toString() ?: ""
                item.findViewById<TextView>(R.id.tvJoint).text =
                    extractLabel(payloadStatus).ifBlank { "Exercise session" }

                val status = session["status"]?.toString() ?: "completed"
                item.findViewById<TextView>(R.id.tvStatus).text =
                    status.replaceFirstChar { it.uppercase() }

                list.addView(item)
            }

            // Wrap list in card background
            list.background = getDrawable(R.drawable.bg_plan_card_default)
        }
    }

    private fun parseTimestamp(s: Map<String, Any?>): Long? {
        val raw = (s["created_at"] ?: s["start_time"] ?: s["date"])?.toString() ?: return null
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )
        for (fmt in formats) { try { return fmt.parse(raw)?.time } catch (_: Exception) {} }
        return null
    }

    private fun extractLabel(payloadStatus: String): String {
        if (payloadStatus.isBlank()) return ""
        val known = listOf("knee", "elbow", "shoulder", "hip", "wrist", "ankle")
        val lower = payloadStatus.lowercase()
        val joint = known.firstOrNull { lower.contains(it) }
            ?: payloadStatus.replace("_", " ").split("-").firstOrNull()?.trim()
        return joint?.replaceFirstChar { it.uppercase() } ?: ""
    }
}
