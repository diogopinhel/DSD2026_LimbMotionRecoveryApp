package com.example.limbmotionrecoveryapp.screens.progress

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.sensor.SensorActivity

class ProgressFragment : Fragment() {

    private lateinit var vm: ProgressViewModel

    private lateinit var progressSpinner: View
    private lateinit var scrollContent: View
    private lateinit var tvWeekLabel: TextView
    private lateinit var tvSessionsCount: TextView
    private lateinit var tvSessionsDelta: TextView
    private lateinit var tvAvgPain: TextView
    private lateinit var tvPainDelta: TextView
    private lateinit var barChartContainer: LinearLayout
    private lateinit var painLineChart: PainLineChartView
    private lateinit var btnConnectSensor: View
    private lateinit var streakBanner: View
    private lateinit var tvStreakNum: TextView
    private lateinit var tvStreakTitle: TextView

    private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressSpinner = view.findViewById(R.id.progressSpinner)
        scrollContent = view.findViewById(R.id.scrollContent)
        tvWeekLabel = view.findViewById(R.id.tvWeekLabel)
        tvSessionsCount = view.findViewById(R.id.tvSessionsCount)
        tvSessionsDelta = view.findViewById(R.id.tvSessionsDelta)
        tvAvgPain = view.findViewById(R.id.tvAvgPain)
        tvPainDelta = view.findViewById(R.id.tvPainDelta)
        barChartContainer = view.findViewById(R.id.barChartContainer)
        painLineChart = view.findViewById(R.id.painLineChart)
        btnConnectSensor = view.findViewById(R.id.btnConnectSensor)
        streakBanner = view.findViewById(R.id.streakBanner)
        tvStreakNum = view.findViewById(R.id.tvStreakNum)
        tvStreakTitle = view.findViewById(R.id.tvStreakTitle)

        vm = ViewModelProvider(this)[ProgressViewModel::class.java]
        vm.state.observe(viewLifecycleOwner) { render(it) }

        btnConnectSensor.setOnClickListener {
            startActivity(Intent(requireContext(), SensorActivity::class.java))
        }

        loadData()
    }

    private fun loadData() {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val userId = prefs.getInt("userId", 0)
        vm.load(userId, token)
    }

    private fun render(state: ProgressViewModel.ProgressState) {
        if (state.loading) {
            progressSpinner.visibility = View.VISIBLE
            scrollContent.visibility = View.GONE
            return
        }
        progressSpinner.visibility = View.GONE
        scrollContent.visibility = View.VISIBLE

        tvWeekLabel.text = state.weekLabel

        tvSessionsCount.text = state.sessionsThisWeek.toString()
        tvSessionsDelta.text = when {
            state.sessionsVsLastWeek > 0 -> "↑ ${state.sessionsVsLastWeek} more than last week"
            state.sessionsVsLastWeek < 0 -> "↓ ${-state.sessionsVsLastWeek} less than last week"
            else -> "Same as last week"
        }
        tvSessionsDelta.setTextColor(
            if (state.sessionsVsLastWeek >= 0) Color.parseColor("#1D9E75")
            else Color.parseColor("#C0392B")
        )

        if (state.avgPainThisWeek > 0f) {
            tvAvgPain.text = "%.1f".format(state.avgPainThisWeek)
            tvPainDelta.text = when {
                state.avgPainLastWeek <= 0f -> "No data last week"
                state.avgPainThisWeek < state.avgPainLastWeek ->
                    "↓ Down from %.1f".format(state.avgPainLastWeek)
                state.avgPainThisWeek > state.avgPainLastWeek ->
                    "↑ Up from %.1f".format(state.avgPainLastWeek)
                else -> "Same as last week"
            }
            tvPainDelta.setTextColor(
                if (state.avgPainThisWeek <= state.avgPainLastWeek || state.avgPainLastWeek <= 0f)
                    Color.parseColor("#1D9E75")
                else Color.parseColor("#C0392B")
            )
        } else {
            tvAvgPain.text = "—"
            tvPainDelta.text = "No check-ins this week"
            tvPainDelta.setTextColor(Color.parseColor("#9EB5AF"))
        }

        buildBarChart(state.sessionsByDay, state.todayDayIndex)
        painLineChart.setData(state.painTrend)

        if (state.streakDays >= 2) {
            streakBanner.visibility = View.VISIBLE
            tvStreakNum.text = state.streakDays.toString()
            tvStreakTitle.text = "${state.streakDays}-day streak"
        } else {
            streakBanner.visibility = View.GONE
        }
    }

    private fun buildBarChart(byDay: List<Int>, todayIndex: Int) {
        barChartContainer.removeAllViews()
        val density = resources.displayMetrics.density
        val maxBarDp = 48
        val minBarDp = 8
        val barWidthDp = 6
        val cornerDp = 3f

        dayNames.forEachIndexed { i, name ->
            val count = if (i < byDay.size) byDay[i] else 0
            val isToday = i == todayIndex
            val isPast = i < todayIndex
            val hasActivity = count > 0

            val col = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            }

            // Spacer
            val spacer = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }
            col.addView(spacer)

            // Bar
            val barHeightDp = if (hasActivity) maxBarDp else minBarDp
            val barColor = when {
                isToday && hasActivity -> Color.parseColor("#0F6E56")
                isPast && hasActivity -> Color.parseColor("#A8DBC9")
                else -> Color.parseColor("#E8EFED")
            }
            val barDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(barColor)
                cornerRadius = cornerDp * density
            }
            val bar = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (barWidthDp * density).toInt(),
                    (barHeightDp * density).toInt()
                ).also { it.bottomMargin = (4 * density).toInt() }
                background = barDrawable
            }
            col.addView(bar)

            // Day label
            val label = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = name
                textSize = 10f
                setTextColor(
                    if (isToday) Color.parseColor("#0F6E56")
                    else Color.parseColor("#9EB5AF")
                )
                if (isToday) setTypeface(null, android.graphics.Typeface.BOLD)
            }
            col.addView(label)

            barChartContainer.addView(col)
        }
    }
}
