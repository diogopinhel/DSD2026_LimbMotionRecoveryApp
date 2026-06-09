package com.example.limbmotionrecoveryapp.screens.session

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.session.SessionController
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SessionSummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_summary)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            SessionController.getInstance(applicationContext).reset()
            finish()
        }

        val sessionId = intent.getIntExtra("sessionId", 0)
        val sampleCount = intent.getIntExtra("sampleCount", 0)
        val errorCount = intent.getIntExtra("errorCount", 0)
        val startTime = intent.getStringExtra("startTime") ?: "--"
        val endTime = intent.getStringExtra("endTime") ?: "--"
        val exerciseType = intent.getStringExtra("exerciseType") ?: ""

        findViewById<TextView>(R.id.tvTitle).text = "Session Summary"
        findViewById<TextView>(R.id.tvExerciseType).text = exerciseType.replaceFirstChar { it.uppercase() }
        findViewById<TextView>(R.id.tvSessionId).text = "Session #$sessionId"
        findViewById<TextView>(R.id.tvSampleCount).text = sampleCount.toString()
        findViewById<TextView>(R.id.tvErrorCount).text = errorCount.toString()
        findViewById<TextView>(R.id.tvStartTime).text = formatIsoTime(startTime)
        findViewById<TextView>(R.id.tvEndTime).text = formatIsoTime(endTime)

        findViewById<MaterialButton>(R.id.btnSaveAndContinue).setOnClickListener {
            SessionController.getInstance(applicationContext).reset()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val controller = SessionController.getInstance(applicationContext)
        if (controller.getState() == SessionController.State.ENDED) {
            controller.reset()
        }
    }

    private fun formatIsoTime(iso: String): String {
        if (iso == "--" || iso.isEmpty()) return "--"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(iso.take(19))
            val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            formatter.format(date)
        } catch (_: Exception) {
            "--"
        }
    }
}
