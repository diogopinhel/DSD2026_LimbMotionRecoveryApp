package com.example.limbmotionrecoveryapp.screens.session

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.sensor.SensorActivity
import com.example.limbmotionrecoveryapp.sensor.SensorRepository
import com.example.limbmotionrecoveryapp.session.SessionController
import com.example.limbmotionrecoveryapp.view.LegView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionPlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXERCISE_NAME = "exercise_name"
        const val EXTRA_EXERCISE_SUB = "exercise_sub"
        const val EXTRA_EXERCISE_DESCRIPTION = "exercise_description"
        const val EXTRA_EXERCISE_GIF_URL = "exercise_gif_url"
        const val EXTRA_EXERCISE_SETS = "exercise_sets"
        const val EXTRA_EXERCISE_REPS = "exercise_reps"
        const val EXTRA_EXERCISE_HOLD = "exercise_hold"
    }

    private lateinit var controller: SessionController
    private lateinit var legView: LegView
    private lateinit var tvExerciseType: TextView
    private lateinit var tvExerciseSub: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLeftAngle: TextView
    private lateinit var tvRightAngle: TextView
    private lateinit var tvRightAngleSub: TextView
    private lateinit var tvAiFeedback: TextView
    private lateinit var tvAiFeedbackLabel: TextView
    private lateinit var tvExerciseCardTitle: TextView
    private lateinit var tvExerciseInstruction: TextView
    private lateinit var pillSets: TextView
    private lateinit var pillReps: TextView
    private lateinit var pillHold: TextView
    private lateinit var cardSensorConnected: MaterialCardView
    private lateinit var cardSensorDisconnected: MaterialCardView
    private lateinit var btnPauseResume: MaterialButton
    private lateinit var btnStop: MaterialButton

    private val colorGreen by lazy { ContextCompat.getColor(this, R.color.colorPrimaryGreen) }
    private val colorGreenDark by lazy { ContextCompat.getColor(this, R.color.colorGreenDark) }
    private val colorGreenExtraLight by lazy { ContextCompat.getColor(this, R.color.colorGreenExtraLight) }
    private val colorAmber by lazy { ContextCompat.getColor(this, R.color.colorAmber) }
    private val colorAmberDark by lazy { ContextCompat.getColor(this, R.color.colorAmberDark) }
    private val colorAmberBg by lazy { ContextCompat.getColor(this, R.color.colorAmberBg) }
    private val colorTextDark by lazy { ContextCompat.getColor(this, R.color.colorTextDark) }
    private val colorTextLight by lazy { ContextCompat.getColor(this, R.color.colorTextLight) }

    private val handler = Handler(Looper.getMainLooper())
    private val lastAngleTimestamps = mutableMapOf<String, Long>()

    private var sessionStartTime = 0L
    private var elapsedBeforePause = 0L
    private var lastResumeTime = 0L
    private var isRunning = true

    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            if (isRunning || controller.getState() == SessionController.State.RUNNING) {
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_player)

        controller = SessionController.getInstance(applicationContext)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@SessionPlayerActivity,
                    "Please click Stop to finish the session",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        initViews()
        loadExerciseInfo()
        setupSensorSection()
        observeSensorState()
        setupLegView()
        checkControllerState()
        startTimer()

        btnPauseResume.setOnClickListener { togglePauseResume() }
        btnStop.setOnClickListener { stopSession() }

        handler.post(uiUpdateRunnable)
    }

    private fun initViews() {
        legView = findViewById(R.id.legView)
        tvExerciseType = findViewById(R.id.tvExerciseType)
        tvExerciseSub = findViewById(R.id.tvExerciseSub)
        tvTimer = findViewById(R.id.tvTimer)
        tvStatus = findViewById(R.id.tvStatus)
        tvLeftAngle = findViewById(R.id.tvLeftAngle)
        tvRightAngle = findViewById(R.id.tvRightAngle)
        tvRightAngleSub = findViewById(R.id.tvRightAngleSub)
        tvAiFeedback = findViewById(R.id.tvAiFeedback)
        tvAiFeedbackLabel = findViewById(R.id.tvAiFeedbackLabel)
        tvExerciseCardTitle = findViewById(R.id.tvExerciseCardTitle)
        tvExerciseInstruction = findViewById(R.id.tvExerciseInstruction)
        pillSets = findViewById(R.id.pillSets)
        pillReps = findViewById(R.id.pillReps)
        pillHold = findViewById(R.id.pillHold)
        cardSensorConnected = findViewById(R.id.cardSensorConnected)
        cardSensorDisconnected = findViewById(R.id.cardSensorDisconnected)
        btnPauseResume = findViewById(R.id.btnPauseResume)
        btnStop = findViewById(R.id.btnStop)

        findViewById<FrameLayout>(R.id.btnBackDisabled).setOnClickListener {
            Toast.makeText(this, "Please click Stop to finish the session", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadExerciseInfo() {
        val name = intent.getStringExtra(EXTRA_EXERCISE_NAME)
            ?: getExerciseDisplayName(controller.getCurrentExerciseType())
        val sub = intent.getStringExtra(EXTRA_EXERCISE_SUB) ?: "—"
        val description = intent.getStringExtra(EXTRA_EXERCISE_DESCRIPTION)
        val gifUrl = intent.getStringExtra(EXTRA_EXERCISE_GIF_URL)
        val sets = intent.getIntExtra(EXTRA_EXERCISE_SETS, 0)
        val reps = intent.getIntExtra(EXTRA_EXERCISE_REPS, 0)
        val hold = intent.getIntExtra(EXTRA_EXERCISE_HOLD, 0)

        tvExerciseType.text = name
        tvExerciseSub.text = sub
        tvExerciseCardTitle.text = name
        tvExerciseInstruction.text = description
            ?.takeIf { it.isNotBlank() }
            ?: "Follow the movement shown above and maintain smooth, controlled motion throughout."

        pillSets.text = if (sets > 0) "$sets Sets" else "—"
        pillReps.text = if (reps > 0) "$reps Reps" else "—"
        if (hold > 0) {
            pillHold.visibility = View.VISIBLE
            pillHold.text = "Hold ${hold}s"
        }

        if (!gifUrl.isNullOrBlank()) {
            Glide.with(this)
                .asGif()
                .load(gifUrl)
                .centerCrop()
                .placeholder(android.R.color.transparent)
                .into(findViewById(R.id.ivExerciseGif))
        }
    }

    private fun setupSensorSection() {
        findViewById<MaterialButton>(R.id.btnConnectSensor).setOnClickListener {
            startActivity(Intent(this, SensorActivity::class.java))
        }
    }

    private fun observeSensorState() {
        SensorRepository.state.observe(this) { state ->
            val connected = state == SensorRepository.State.CONNECTED
            cardSensorConnected.visibility = if (connected) View.VISIBLE else View.GONE
            cardSensorDisconnected.visibility = if (connected) View.GONE else View.VISIBLE
        }
    }

    private fun setupLegView() {
        val exerciseType = controller.getCurrentExerciseType()
        val mode = when (exerciseType) {
            "lying_flat" -> LegView.DisplayMode.HORIZONTAL
            "squat" -> LegView.DisplayMode.SQUAT
            "march_in_place" -> LegView.DisplayMode.STEPPING
            else -> LegView.DisplayMode.HORIZONTAL
        }
        legView.setDisplayMode(mode)
        legView.setAngleModeDegrees()

        legView.setLeftLimbColors(intArrayOf(
            Color.parseColor("#FFE4D6"),
            Color.parseColor("#F5C6A5"),
            Color.parseColor("#D4A373")
        ))
        legView.setLeftCapColors(intArrayOf(
            Color.parseColor("#FFF0E6"),
            Color.parseColor("#D4A373")
        ))
        legView.setRightLimbColors(intArrayOf(
            Color.parseColor("#E8B89A"),
            Color.parseColor("#C6865C"),
            Color.parseColor("#8B5E3C")
        ))
        legView.setRightCapColors(intArrayOf(
            Color.parseColor("#F5D0B5"),
            Color.parseColor("#B07D4B")
        ))
    }

    private fun checkControllerState() {
        when (controller.getState()) {
            SessionController.State.IDLE -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = controller.start()
                    withContext(Dispatchers.Main) {
                        result.onFailure { e ->
                            Toast.makeText(this@SessionPlayerActivity, "Start failed: ${e.message}", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
            }
            SessionController.State.ENDED -> {
                Toast.makeText(this, "Session already ended", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            else -> { }
        }
    }

    private fun startTimer() {
        sessionStartTime = System.currentTimeMillis()
        lastResumeTime = sessionStartTime
    }

    private fun updateUI() {
        val now = System.currentTimeMillis()

        val elapsed = if (isRunning) {
            elapsedBeforePause + (now - lastResumeTime)
        } else {
            elapsedBeforePause
        }
        tvTimer.text = formatDuration(elapsed)

        val controllerState = controller.getState()
        val sessionRunning = controllerState == SessionController.State.RUNNING
        tvStatus.text = if (sessionRunning) "RUNNING" else "PAUSED"
        tvStatus.setTextColor(if (sessionRunning) colorGreenDark else colorAmberDark)
        tvStatus.backgroundTintList = ColorStateList.valueOf(
            if (sessionRunning) colorGreenExtraLight else colorAmberBg
        )

        val data = controller.getLatestData()
        data?.targetAngles?.forEach { angle ->
            lastAngleTimestamps[angle.angleID] = angle.timestamp
        }

        val leftEntry = lastAngleTimestamps.entries.findLast { it.key.contains("left", ignoreCase = true) }
        val leftVisible = leftEntry != null && (now - leftEntry.value < 60_000)
        val leftAngle = if (leftVisible) {
            data?.targetAngles?.findLast { it.angleID.contains("left", ignoreCase = true) }?.angle?.toFloat()
        } else null

        val rightEntry = lastAngleTimestamps.entries.findLast { it.key.contains("right", ignoreCase = true) }
        val rightVisible = rightEntry != null && (now - rightEntry.value < 60_000)
        val rightAngle = if (rightVisible) {
            data?.targetAngles?.findLast { it.angleID.contains("right", ignoreCase = true) }?.angle?.toFloat()
        } else null

        legView.setShowLeft(leftVisible)
        legView.setShowRight(rightVisible)
        leftAngle?.let { legView.setLeftAngle((180f - it).coerceIn(0f, 180f)) }
        rightAngle?.let { legView.setRightAngle((180f - it).coerceIn(0f, 180f)) }

        tvLeftAngle.text = leftAngle?.let { "%.1f°".format(it) } ?: "--"
        tvRightAngle.text = rightAngle?.let { "%.1f°".format(it) } ?: "--"
        tvRightAngleSub.text = if (rightAngle != null) "Current angle" else "Not connected"
        tvRightAngle.setTextColor(if (rightAngle != null) colorTextDark else colorTextLight)

        val aiRecs = controller.getLatestLiveRecommendations()
        tvAiFeedback.text = if (aiRecs.isNotEmpty()) {
            aiRecs.joinToString("\n") { rec ->
                val joint = rec["joint"] as? String ?: ""
                val suggestion = rec["suggestion"] as? String ?: ""
                val priority = rec["priority"] as? String ?: ""
                if (joint.isNotEmpty() && suggestion.isNotEmpty()) "[$priority] $joint: $suggestion" else ""
            }.trim().ifBlank { "None" }
        } else {
            "None"
        }
    }

    private fun togglePauseResume() {
        if (isRunning) {
            controller.pause()
            elapsedBeforePause += System.currentTimeMillis() - lastResumeTime
            isRunning = false
            btnPauseResume.text = "Resume"
            btnPauseResume.backgroundTintList = ColorStateList.valueOf(colorGreen)
        } else {
            controller.resume()
            lastResumeTime = System.currentTimeMillis()
            isRunning = true
            btnPauseResume.text = "Pause"
            btnPauseResume.backgroundTintList = ColorStateList.valueOf(colorAmber)
            handler.post(uiUpdateRunnable)
        }
    }

    private fun stopSession() {
        btnPauseResume.isEnabled = false
        btnStop.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val result = controller.stop()
            withContext(Dispatchers.Main) {
                result.onSuccess { summary ->
                    handler.removeCallbacks(uiUpdateRunnable)
                    val intent = Intent(this@SessionPlayerActivity, SessionSummaryActivity::class.java).apply {
                        putExtra("sessionId", summary.sessionId)
                        putExtra("sampleCount", summary.sampleCount)
                        putExtra("errorCount", summary.errorCount)
                        putExtra("startTime", summary.startTime)
                        putExtra("endTime", summary.endTime)
                        putExtra("exerciseType", summary.exerciseType)
                    }
                    startActivity(intent)
                    finish()
                }.onFailure { e ->
                    btnPauseResume.isEnabled = true
                    btnStop.isEnabled = true
                    Toast.makeText(this@SessionPlayerActivity, "Stop failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun getExerciseDisplayName(type: String): String = when (type) {
        "lying_flat" -> "Lying Flat"
        "squat" -> "Squat"
        "march_in_place" -> "March in Place"
        "bend_knee_10" -> "Bend Knee"
        else -> type.replaceFirstChar { it.uppercase() }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(uiUpdateRunnable)
    }
}
