package com.example.limbmotionrecoveryapp.screens.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.screens.session.PrepareExerciseActivity
import com.example.limbmotionrecoveryapp.sensor.SensorRepository
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

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

        tvGreeting.text = buildGreeting(userName)

        // 修复：Start Exercises 跳转到 Session 准备页面
        btnStartExercises.setOnClickListener {
            val intent = Intent(requireContext(), PrepareExerciseActivity::class.java)
            // 可选：传递当前 pending plan 名称
            val planName = viewModel.state.value?.activePlanName
            if (!planName.isNullOrBlank()) {
                intent.putExtra("planName", planName)
            }
            startActivity(intent)
        }

        btnConnectSensor.setOnClickListener {
            // 保持原有传感器连接逻辑
            // startActivity(Intent(requireContext(), SensorActivity::class.java))
        }

        SensorRepository.state.observe(viewLifecycleOwner) { repoState ->
            val connected = repoState == SensorRepository.State.CONNECTED
            viewModel.setSensorConnected(connected)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            // Subline: plan name + week
            tvPlanSubline.text = when {
                state.activePlanName != null && state.recoveryWeekTotal > 0 ->
                    "${state.activePlanName} · Week ${state.recoveryWeekCurrent} of ${state.recoveryWeekTotal}"
                state.activePlanName != null -> state.activePlanName
                state.loading -> ""
                else -> "No active plan"
            }

            // Week progress bar
            if (state.recoveryWeekTotal > 0) {
                tvWeekLabel.text = "${state.recoveryWeekCurrent} of ${state.recoveryWeekTotal} weeks"
                weekProgressBar.progress = (state.weekProgressFraction * 100).toInt()
            } else {
                tvWeekLabel.text = "—"
                weekProgressBar.progress = 0
            }

            // Sensor banner
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

            // Today at a glance
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

    private fun buildGreeting(name: String): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
        val firstName = name.split(" ").first().takeIf { it.isNotBlank() }
        return if (firstName != null) "$timeGreeting, $firstName 👋" else timeGreeting
    }
}