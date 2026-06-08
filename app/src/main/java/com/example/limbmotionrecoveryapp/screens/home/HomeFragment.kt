package com.example.limbmotionrecoveryapp.screens.home

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.sensor.SensorActivity
import com.example.limbmotionrecoveryapp.sensor.SensorRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var btnPainCheckIn: LinearLayout
    private lateinit var tvPainBtnText: TextView
    private lateinit var tvPainCardNumber: TextView
    private lateinit var tvPainCardLabel: TextView
    private lateinit var tvPainCardSubText: TextView
    private lateinit var vPainBarTrack: View
    private lateinit var vPainBarMarker: View

    private val handler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (isAdded) refreshPainButton()
            handler.postDelayed(this, 60_000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val userId = prefs.getInt("userId", 0)
        val userName = prefs.getString("userName", "") ?: ""

        val tvGreeting = view.findViewById<TextView>(R.id.tvGreeting)
        val tvPlanSubline = view.findViewById<TextView>(R.id.tvPlanSubline)
        val tvWeekLabel = view.findViewById<TextView>(R.id.tvWeekLabel)
        val weekProgressBar = view.findViewById<ProgressBar>(R.id.weekProgressBar)
        val sensorBannerOff = view.findViewById<LinearLayout>(R.id.sensorBannerOff)
        val sensorBannerOn = view.findViewById<LinearLayout>(R.id.sensorBannerOn)
        val tvSensorBattery = view.findViewById<TextView>(R.id.tvSensorBattery)
        val sensorLiveCard = view.findViewById<LinearLayout>(R.id.sensorLiveCard)
        val tvLiveRom = view.findViewById<TextView>(R.id.tvLiveRom)
        val tvLiveMovement = view.findViewById<TextView>(R.id.tvLiveMovement)
        val tvGlanceSessions = view.findViewById<TextView>(R.id.tvGlanceSessions)
        val tvGlanceSessionsSub = view.findViewById<TextView>(R.id.tvGlanceSessionsSub)
        val tvGlanceProgress = view.findViewById<TextView>(R.id.tvGlanceProgress)
        val tvGlanceProgressSub = view.findViewById<TextView>(R.id.tvGlanceProgressSub)
        val btnStartExercises = view.findViewById<LinearLayout>(R.id.btnStartExercises)
        val btnConnectSensor = view.findViewById<TextView>(R.id.btnConnectSensor)
        btnPainCheckIn = view.findViewById(R.id.btnPainCheckIn)
        tvPainBtnText = view.findViewById(R.id.tvPainBtnText)
        tvPainCardNumber = view.findViewById(R.id.tvPainCardNumber)
        tvPainCardLabel = view.findViewById(R.id.tvPainCardLabel)
        tvPainCardSubText = view.findViewById(R.id.tvPainCardSubText)
        vPainBarTrack = view.findViewById(R.id.vPainBarTrack)
        vPainBarMarker = view.findViewById(R.id.vPainBarMarker)

        setupLearnSection(view)

        tvGreeting.text = buildGreeting(userName)

        btnStartExercises.setOnClickListener {
            requireActivity()
                .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                    R.id.bottomNav
                ).selectedItemId = R.id.nav_plans
        }

        btnConnectSensor.setOnClickListener {
            startActivity(Intent(requireContext(), SensorActivity::class.java))
        }

        btnPainCheckIn.setOnClickListener {
            if (!isLockedToday()) {
                startActivity(Intent(requireContext(), PainCheckInActivity::class.java))
            }
        }

        view.findViewById<TextView>(R.id.tvPainViewLink).setOnClickListener {
            startActivity(Intent(requireContext(), PainCheckInActivity::class.java))
        }

        SensorRepository.state.observe(viewLifecycleOwner) { repoState ->
            val connected = repoState == SensorRepository.State.CONNECTED
            viewModel.setSensorConnected(connected)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            tvPlanSubline.text = when {
                state.activePlanName != null -> state.activePlanName
                state.loading -> ""
                else -> "No active plan"
            }

            if (state.totalSessions > 0) {
                tvWeekLabel.text = "${state.completedSessions} / ${state.totalSessions}"
                weekProgressBar.progress = state.completedSessions * 100 / state.totalSessions
            } else {
                tvWeekLabel.text = "—"
                weekProgressBar.progress = 0
            }

            if (state.sensorConnected) {
                sensorBannerOff.visibility = View.GONE
                sensorBannerOn.visibility = View.VISIBLE
                sensorLiveCard.visibility = View.VISIBLE
                tvSensorBattery.text = if (state.sensorBatteryPct > 0)
                    "Battery ${state.sensorBatteryPct}% · Tracking ready"
                else
                    "Tracking ready"
                state.liveRomDeg?.let { tvLiveRom.text = "%.1f°".format(it) }
                state.liveMovementMs2?.let { tvLiveMovement.text = "%.1f".format(it) }
            } else {
                sensorBannerOff.visibility = View.VISIBLE
                sensorBannerOn.visibility = View.GONE
                sensorLiveCard.visibility = View.GONE
            }

            if (state.totalSessions > 0) {
                tvGlanceSessions.text = "${state.completedSessions}/${state.totalSessions}"
                tvGlanceSessionsSub.text = "exercises completed"
                val pct = state.completedSessions * 100 / state.totalSessions
                tvGlanceProgress.text = "$pct%"
                tvGlanceProgressSub.text = "of plan complete"
            } else {
                tvGlanceSessions.text = "—"
                tvGlanceSessionsSub.text = " "
                tvGlanceProgress.text = "—"
                tvGlanceProgressSub.text = " "
            }
        }

        if (token.isNotBlank() && userId > 0) {
            viewModel.load(userId, token, userName)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::btnPainCheckIn.isInitialized) {
            refreshPainButton()
            if (isLockedToday()) handler.post(countdownRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(countdownRunnable)
    }

    private fun refreshPainButton() {
        val entry = getLastPainEntry()
        val lockedToday = entry != null && isSameDay(entry.timestamp)

        if (entry != null) {
            val color = Color.parseColor(painColor(entry.level))
            tvPainCardNumber.text = entry.level.toString()
            tvPainCardLabel.text = painLabel(entry.level)
            tvPainCardNumber.setTextColor(color)
            tvPainCardLabel.setTextColor(color)
            positionPainMarker(entry.level)
        } else {
            tvPainCardNumber.text = "—"
            tvPainCardLabel.text = ""
            tvPainCardNumber.setTextColor(resources.getColor(R.color.colorAmberText, null))
            vPainBarMarker.visibility = View.GONE
        }

        if (lockedToday) {
            btnPainCheckIn.setBackgroundResource(R.drawable.bg_cta_btn_locked)
            val ms = msUntilMidnight()
            val h = ms / 3_600_000
            val m = (ms % 3_600_000) / 60_000
            tvPainBtnText.text = "🔒  Next check-in in ${h}h ${m}m"
            tvPainCardSubText.text = "Pain registered for today"
        } else {
            btnPainCheckIn.setBackgroundResource(R.drawable.bg_cta_btn)
            tvPainBtnText.text = "Register today's pain"
            tvPainCardSubText.text = if (entry != null)
                "Last: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(entry.timestamp))}"
            else
                "Tap to register"
        }
    }

    private fun positionPainMarker(level: Int) {
        vPainBarMarker.visibility = View.VISIBLE
        vPainBarTrack.post {
            val trackW = vPainBarTrack.width
            if (trackW <= 0) return@post
            val markerW = vPainBarMarker.width.takeIf { it > 0 } ?: 9  // 3dp in px (~)
            val pct = level / 10f
            vPainBarMarker.translationX = trackW * pct - markerW / 2f
        }
    }

    private fun getLastPainEntry(): PainCheckInActivity.PainEntry? {
        val json = requireContext()
            .getSharedPreferences("pain_prefs", Context.MODE_PRIVATE)
            .getString("entries", null) ?: return null
        return try {
            val type = object : TypeToken<List<PainCheckInActivity.PainEntry>>() {}.type
            val entries: List<PainCheckInActivity.PainEntry> = Gson().fromJson(json, type)
            entries.firstOrNull()
        } catch (e: Exception) { null }
    }

    private fun isLockedToday(): Boolean {
        val entry = getLastPainEntry() ?: return false
        return isSameDay(entry.timestamp)
    }

    private fun isSameDay(timestamp: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp)) == sdf.format(Date())
    }

    private fun painLabel(level: Int) = when (level) {
        1 -> "No pain"
        2, 3 -> "Minimal"
        4, 5 -> "Mild"
        6, 7 -> "Moderate"
        8, 9 -> "Severe"
        10 -> "Worst possible"
        else -> ""
    }

    private fun painColor(level: Int) = when {
        level <= 3 -> "#1D9E75"
        level <= 6 -> "#BA7517"
        else -> "#C0392B"
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

    private fun setupLearnSection(view: View) {
        val tutorials = LearnTutorials.all.take(4)
        val container = view.findViewById<LinearLayout>(R.id.learnCardsContainer)
        val inflater = LayoutInflater.from(requireContext())
        val gap = (8 * resources.displayMetrics.density).toInt()

        tutorials.forEachIndexed { index, tutorial ->
            val cardView = inflater.inflate(R.layout.item_learn_card, container, false)
            cardView.findViewById<View>(R.id.learnThumbBg).setBackgroundColor(Color.parseColor(tutorial.thumbColor))
            cardView.findViewById<TextView>(R.id.learnTitle).text = tutorial.title
            cardView.findViewById<TextView>(R.id.learnDuration).text = tutorial.duration
            if (index > 0) {
                (cardView.layoutParams as LinearLayout.LayoutParams).marginStart = gap
            }
            cardView.setOnClickListener { openTutorial(tutorial) }
            container.addView(cardView)
        }

        view.findViewById<TextView>(R.id.btnLearnSeeAll).setOnClickListener {
            startActivity(Intent(requireContext(), LearnAllActivity::class.java))
        }
    }

    private fun openTutorial(tutorial: LearnTutorials.Tutorial) {
        val intent = Intent(requireContext(), LearnTutorialActivity::class.java).apply {
            putExtra(LearnTutorialActivity.EXTRA_TITLE, tutorial.title)
            putExtra(LearnTutorialActivity.EXTRA_DESCRIPTION, tutorial.description)
            putExtra(LearnTutorialActivity.EXTRA_DURATION, tutorial.duration)
            putExtra(LearnTutorialActivity.EXTRA_YOUTUBE_ID, tutorial.youtubeId)
            putExtra(LearnTutorialActivity.EXTRA_CATEGORY, tutorial.category)
        }
        startActivity(intent)
    }

    private fun buildGreeting(name: String): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
        val firstName = name.split(" ").first().takeIf { it.isNotBlank() }
        return if (firstName != null) "$timeGreeting, $firstName 👋" else timeGreeting
    }
}
