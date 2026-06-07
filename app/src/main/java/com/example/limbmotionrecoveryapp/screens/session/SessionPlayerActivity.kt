package com.example.limbmotionrecoveryapp.screens.session

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.session.SessionController
import com.example.limbmotionrecoveryapp.view.LegView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionPlayerActivity : AppCompatActivity() {

    private lateinit var controller: SessionController
    private lateinit var legView: LegView
    private lateinit var tvExerciseType: TextView
    private lateinit var tvSessionId: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLeftAngle: TextView
    private lateinit var tvRightAngle: TextView
    private lateinit var tvAiFeedback: TextView
    private lateinit var tvAiFeedbackLabel: TextView
    private lateinit var btnPauseResume: MaterialButton
    private lateinit var btnStop: MaterialButton

    // [WSS] 新增：实时反馈显示
    private lateinit var tvIsCorrect: TextView

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
        tvSessionId = findViewById(R.id.tvSessionId)
        tvTimer = findViewById(R.id.tvTimer)
        tvStatus = findViewById(R.id.tvStatus)
        tvLeftAngle = findViewById(R.id.tvLeftAngle)
        tvRightAngle = findViewById(R.id.tvRightAngle)
        tvAiFeedback = findViewById(R.id.tvAiFeedback)
        tvAiFeedbackLabel = findViewById(R.id.tvAiFeedbackLabel)
        btnPauseResume = findViewById(R.id.btnPauseResume)
        btnStop = findViewById(R.id.btnStop)

        // [WSS] 新增
        tvIsCorrect = findViewById(R.id.tvIsCorrect)

        val exerciseType = controller.getCurrentExerciseType()
        tvExerciseType.text = getExerciseDisplayName(exerciseType)
        tvSessionId.text = "Session #${controller.getCurrentSessionId()}"
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
        tvStatus.text = when (controllerState) {
            SessionController.State.RUNNING -> "RUNNING"
            SessionController.State.PAUSED -> "PAUSED"
            else -> "—"
        }
        tvStatus.setTextColor(
            if (controllerState == SessionController.State.RUNNING)
                ContextCompat.getColor(this, R.color.colorPrimaryGreen)
            else
                ContextCompat.getColor(this, R.color.colorAmber)
        )

        // ====== 原有 S2 本地传感器数据逻辑（完全保留） ======
        val data = controller.getLatestData()
        data?.targetAngles?.forEach { angle ->
            lastAngleTimestamps[angle.angleID] = angle.timestamp
        }

        val leftEntry = lastAngleTimestamps.entries.findLast {
            it.key.contains("left", ignoreCase = true)
        }
        val leftVisible = leftEntry != null && (now - leftEntry.value < 60_000)
        val leftAngle = if (leftVisible) {
            data?.targetAngles?.findLast { it.angleID.contains("left", ignoreCase = true) }?.angle?.toFloat()
        } else null

        val rightEntry = lastAngleTimestamps.entries.findLast {
            it.key.contains("right", ignoreCase = true)
        }
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
        // =====================================================

        // [WSS] 新增：用服务端反馈覆盖 last 数据，并显示动作标准状态
        val feedback = controller.getLatestFeedback()
        if (feedback != null) {
            val wssAngle = feedback.angle
            val wssJoint = feedback.joint

            when {
                wssJoint.contains("left", ignoreCase = true) -> {
                    tvLeftAngle.text = "%.1f°".format(wssAngle)
                    legView.setLeftAngle((180f - wssAngle).coerceIn(0f, 180f))
                    legView.setShowLeft(true)
                }
                wssJoint.contains("right", ignoreCase = true) -> {
                    tvRightAngle.text = "%.1f°".format(wssAngle)
                    legView.setRightAngle((180f - wssAngle).coerceIn(0f, 180f))
                    legView.setShowRight(true)
                }
                else -> {
                    // joint 不含 left/right（如 "knee"），更新 S2 当前有数据的那条腿
                    if (leftAngle != null && rightAngle == null) {
                        tvLeftAngle.text = "%.1f°".format(wssAngle)
                        legView.setLeftAngle((180f - wssAngle).coerceIn(0f, 180f))
                        legView.setShowLeft(true)
                    } else if (rightAngle != null && leftAngle == null) {
                        tvRightAngle.text = "%.1f°".format(wssAngle)
                        legView.setRightAngle((180f - wssAngle).coerceIn(0f, 180f))
                        legView.setShowRight(true)
                    } else {
                        // 两边都有或都没有，默认更新左腿
                        tvLeftAngle.text = "%.1f°".format(wssAngle)
                        legView.setLeftAngle((180f - wssAngle).coerceIn(0f, 180f))
                        legView.setShowLeft(true)
                    }
                }
            }

            tvIsCorrect.text = if (feedback.isCorrect) "✅ Standard" else "❌ Adjust"
            tvIsCorrect.setTextColor(
                if (feedback.isCorrect) Color.parseColor("#4CAF50")
                else Color.parseColor("#F44336")
            )
        } else {
            tvIsCorrect.text = "⏳ Analyzing..."
            tvIsCorrect.setTextColor(Color.parseColor("#9E9E9E"))
        }

        // AI Feedback（原有逻辑完全保留）
        val aiRecs = controller.getLatestLiveRecommendations()
        val aiText = if (aiRecs.isNotEmpty()) {
            aiRecs.joinToString("\n") { rec ->
                val joint = rec["joint"] as? String ?: ""
                val suggestion = rec["suggestion"] as? String ?: ""
                val priority = rec["priority"] as? String ?: ""
                if (joint.isNotEmpty() && suggestion.isNotEmpty()) {
                    "[$priority] $joint: $suggestion"
                } else {
                    ""
                }
            }.trim()
        } else {
            "None"
        }
        tvAiFeedback.text = aiText
        tvAiFeedbackLabel.text = "AI Feedback"
    }

    private fun togglePauseResume() {
        val green = ContextCompat.getColor(this, R.color.colorPrimaryGreen)
        val amber = ContextCompat.getColor(this, R.color.colorAmber)

        if (isRunning) {
            controller.pause()
            elapsedBeforePause += System.currentTimeMillis() - lastResumeTime
            isRunning = false

            btnPauseResume.text = "Resume"
            btnPauseResume.backgroundTintList = ColorStateList.valueOf(green)
        } else {
            controller.resume()
            lastResumeTime = System.currentTimeMillis()
            isRunning = true

            btnPauseResume.text = "Pause"
            btnPauseResume.backgroundTintList = ColorStateList.valueOf(amber)
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
                    Snackbar.make(btnStop, "Stop failed: ${e.message}", Snackbar.LENGTH_LONG).show()
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
        else -> type.replaceFirstChar { it.uppercase() }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(uiUpdateRunnable)
    }
}