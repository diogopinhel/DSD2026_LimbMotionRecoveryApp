package com.example.limbmotionrecoveryapp.screens.session

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R

class SessionPlayerActivity : AppCompatActivity() {

    private lateinit var tvExerciseName: TextView
    private lateinit var tvExerciseProgress: TextView
    private lateinit var tvAngleValue: TextView
    private lateinit var tvAngleId: TextView
    private lateinit var btnPause: Button
    private lateinit var btnResume: Button
    private lateinit var btnStop: Button
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button

    private var exerciseList: List<String> = emptyList()
    private var currentExerciseIndex = 0

    // Session state: IDLE, RUNNING, PAUSED, ENDED
    private var sessionState = "IDLE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_player)

        // Get exercise list from intent
        exerciseList = intent.getStringArrayListExtra("exerciseList") ?: emptyList()
        currentExerciseIndex = intent.getIntExtra("exerciseIndex", 0)

        // Init views
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvExerciseProgress = findViewById(R.id.tvExerciseProgress)
        tvAngleValue = findViewById(R.id.tvAngleValue)
        tvAngleId = findViewById(R.id.tvAngleId)
        btnPause = findViewById(R.id.btnPause)
        btnResume = findViewById(R.id.btnResume)
        btnStop = findViewById(R.id.btnStop)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        // Update UI with current exercise
        updateExerciseUI()

        // Start session
        sessionState = "RUNNING"
        updateButtonsForState(sessionState)

        // Button listeners
        btnPause.setOnClickListener {
            sessionState = "PAUSED"
            updateButtonsForState(sessionState)
            // TODO: replace with controller.pause() when SessionController is available
        }

        btnResume.setOnClickListener {
            sessionState = "RUNNING"
            updateButtonsForState(sessionState)
            // TODO: replace with controller.resume() when SessionController is available
        }

        btnStop.setOnClickListener {
            sessionState = "ENDED"
            updateButtonsForState(sessionState)
            // TODO: replace with controller.stop() when SessionController is available
            val intent = Intent(this, SessionSummaryActivity::class.java)
            intent.putExtra("sessionId", 0)
            intent.putExtra("sampleCount", 0)
            intent.putExtra("errorCount", 0)
            intent.putExtra("startTime", "")
            intent.putExtra("endTime", "")
            startActivity(intent)
            finish()
        }

        btnNext.setOnClickListener {
            if (currentExerciseIndex < exerciseList.size - 1) {
                currentExerciseIndex++
                updateExerciseUI()
            } else {
                Toast.makeText(this, "Last exercise!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSkip.setOnClickListener {
            if (currentExerciseIndex < exerciseList.size - 1) {
                currentExerciseIndex++
                updateExerciseUI()
            } else {
                Toast.makeText(this, "Last exercise!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateExerciseUI() {
        val name = if (exerciseList.isNotEmpty())
            exerciseList[currentExerciseIndex] else "Exercise"
        tvExerciseName.text = name
        tvExerciseProgress.text =
            "Exercise ${currentExerciseIndex + 1} of ${exerciseList.size}"
    }

    private fun updateButtonsForState(state: String) {
        when (state) {
            "RUNNING" -> {
                btnPause.visibility = View.VISIBLE
                btnResume.visibility = View.GONE
            }
            "PAUSED" -> {
                btnPause.visibility = View.GONE
                btnResume.visibility = View.VISIBLE
            }
            "ENDED" -> {
                btnPause.visibility = View.GONE
                btnResume.visibility = View.GONE
            }
        }
    }
}