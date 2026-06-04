package com.example.limbmotionrecoveryapp.screens.session

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R

class SessionSummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_summary)

        // Get data from intent
        val sampleCount = intent.getIntExtra("sampleCount", 0)
        val errorCount = intent.getIntExtra("errorCount", 0)
        val startTime = intent.getStringExtra("startTime") ?: "--"
        val endTime = intent.getStringExtra("endTime") ?: "--"

        // Bind views
        val tvSampleCount = findViewById<TextView>(R.id.tvSampleCount)
        val tvErrorCount = findViewById<TextView>(R.id.tvErrorCount)
        val tvStartTime = findViewById<TextView>(R.id.tvStartTime)
        val tvEndTime = findViewById<TextView>(R.id.tvEndTime)
        val btnSaveAndContinue = findViewById<Button>(R.id.btnSaveAndContinue)

        // Populate data
        tvSampleCount.text = sampleCount.toString()
        tvErrorCount.text = errorCount.toString()
        tvStartTime.text = if (startTime.isNotEmpty()) startTime.takeLast(8) else "--:--"
        tvEndTime.text = if (endTime.isNotEmpty()) endTime.takeLast(8) else "--:--"

        // Save & Continue — goes back to Plan Details
        btnSaveAndContinue.setOnClickListener {
            finish()
        }
    }
}