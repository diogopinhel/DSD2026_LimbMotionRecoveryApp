package com.example.limbmotionrecoveryapp.screens.progress

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.limbmotionrecoveryapp.R

class ProgressFragment : Fragment() {

    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val userId = prefs.getInt("userId", 0)

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvStateMessage = view.findViewById<TextView>(R.id.tvStateMessage)
        val scrollContent = view.findViewById<ScrollView>(R.id.scrollContent)
        val tvWeekBadge = view.findViewById<TextView>(R.id.tvWeekBadge)

        val tabOverview = view.findViewById<LinearLayout>(R.id.tabOverview)
        val tabPain = view.findViewById<LinearLayout>(R.id.tabPain)
        val tabRom = view.findViewById<LinearLayout>(R.id.tabRom)
        val tvTabOverview = view.findViewById<TextView>(R.id.tvTabOverview)
        val tvTabPain = view.findViewById<TextView>(R.id.tvTabPain)
        val tvTabRom = view.findViewById<TextView>(R.id.tvTabRom)
        val indicatorOverview = view.findViewById<View>(R.id.indicatorOverview)
        val indicatorPain = view.findViewById<View>(R.id.indicatorPain)
        val indicatorRom = view.findViewById<View>(R.id.indicatorRom)
        val contentOverview = view.findViewById<LinearLayout>(R.id.contentOverview)
        val contentPain = view.findViewById<LinearLayout>(R.id.contentPain)
        val contentRom = view.findViewById<LinearLayout>(R.id.contentRom)

        fun selectTab(index: Int) {
            val active = ContextCompat.getColor(requireContext(), R.color.colorPrimaryGreen)
            val inactive = ContextCompat.getColor(requireContext(), R.color.colorTextLight)
            tvTabOverview.setTextColor(if (index == 0) active else inactive)
            tvTabPain.setTextColor(if (index == 1) active else inactive)
            tvTabRom.setTextColor(if (index == 2) active else inactive)
            indicatorOverview.visibility = if (index == 0) View.VISIBLE else View.INVISIBLE
            indicatorPain.visibility = if (index == 1) View.VISIBLE else View.INVISIBLE
            indicatorRom.visibility = if (index == 2) View.VISIBLE else View.INVISIBLE
            contentOverview.visibility = if (index == 0) View.VISIBLE else View.GONE
            contentPain.visibility = if (index == 1) View.VISIBLE else View.GONE
            contentRom.visibility = if (index == 2) View.VISIBLE else View.GONE
        }

        tabOverview.setOnClickListener { selectTab(0) }
        tabPain.setOnClickListener { selectTab(1) }
        tabRom.setOnClickListener { selectTab(2) }
        selectTab(0)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProgressViewModel.State.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    tvStateMessage.visibility = View.GONE
                    scrollContent.visibility = View.GONE
                }
                is ProgressViewModel.State.EndpointMissing -> {
                    progressBar.visibility = View.GONE
                    tvStateMessage.visibility = View.VISIBLE
                    tvStateMessage.text = state.message
                    scrollContent.visibility = View.GONE
                }
                is ProgressViewModel.State.Error -> {
                    progressBar.visibility = View.GONE
                    tvStateMessage.visibility = View.VISIBLE
                    tvStateMessage.text = state.message
                    scrollContent.visibility = View.GONE
                }
                is ProgressViewModel.State.Success -> {
                    progressBar.visibility = View.GONE
                    tvStateMessage.visibility = View.GONE
                    scrollContent.visibility = View.VISIBLE
                    if (state.data.weekLabel.isNotBlank()) {
                        tvWeekBadge.text = state.data.weekLabel
                        tvWeekBadge.visibility = View.VISIBLE
                    }
                    bindData(view, state.data)
                }
            }
        }

        if (token.isNotBlank() && userId > 0) {
            viewModel.load(userId, token)
        } else {
            progressBar.visibility = View.GONE
            tvStateMessage.visibility = View.VISIBLE
            tvStateMessage.text = "Please log in again"
        }
    }

    private fun bindData(view: View, data: ProgressData) {
        val romChart = view.findViewById<LineChartView>(R.id.romChart)
        val romChart2 = view.findViewById<LineChartView>(R.id.romChart2)

        data.rom?.let { rom ->
            view.findViewById<TextView>(R.id.tvRomValue).text = "${rom.currentDegrees}°"
            view.findViewById<TextView>(R.id.tvRomValue2).text = "${rom.currentDegrees}°"
            view.findViewById<TextView>(R.id.tvRomCurrent).text = "${rom.currentDegrees}°"
            view.findViewById<TextView>(R.id.tvRomTarget).text = "${rom.targetDegrees}°"
            val gain = rom.weeklyGainDegrees
            view.findViewById<TextView>(R.id.tvRomWeeklyGain2).text = if (gain >= 0) "+${gain}°" else "${gain}°"
            if (gain != 0) {
                val gainView = view.findViewById<TextView>(R.id.tvRomGain)
                gainView.text = "↑ ${gain}° improvement from last week"
                gainView.visibility = View.VISIBLE
            }
            if (rom.history.size >= 2) {
                romChart.setData(rom.history, rom.targetDegrees)
                romChart2.setData(rom.history, rom.targetDegrees)
            } else {
                romChart.setNoData()
                romChart2.setNoData()
            }
        } ?: run {
            romChart.setNoData()
            romChart2.setNoData()
        }

        data.adherence?.let { adh ->
            view.findViewById<DonutChartView>(R.id.donutChart).percent = adh.weeklyPercent
            view.findViewById<TextView>(R.id.tvAdherencePct).text = "${adh.weeklyPercent}%"
            view.findViewById<TextView>(R.id.tvAdherenceExercises).text =
                "${adh.completedExercises} of ${adh.totalExercises} exercises"
            if (adh.skippedExercises > 0) {
                view.findViewById<TextView>(R.id.tvAdherenceSkipped).text =
                    "${adh.skippedExercises} skipped this week"
            }
            if (adh.streakWeeks > 0) {
                val streakView = view.findViewById<TextView>(R.id.tvAdherenceStreak)
                streakView.text = "🔥 ${adh.streakWeeks} week streak"
                streakView.visibility = View.VISIBLE
            }
            buildWeekDays(view.findViewById(R.id.weekDaysContainer), adh.weekDays)
        }

        data.pain?.let { pain ->
            val changeText = when {
                pain.changeFromLastWeek < 0 -> "↓ ${-pain.changeFromLastWeek} from last week"
                pain.changeFromLastWeek > 0 -> "↑ ${pain.changeFromLastWeek} from last week"
                else -> "No change from last week"
            }
            val changeColor = if (pain.changeFromLastWeek <= 0)
                ContextCompat.getColor(requireContext(), R.color.colorPrimaryGreen)
            else Color.parseColor("#BA7517")

            listOf(
                R.id.tvPainAvg to R.id.tvPainChange,
                R.id.tvPainAvg2 to R.id.tvPainChange2
            ).forEach { (avgId, changeId) ->
                view.findViewById<TextView>(avgId).text = "${pain.averageThisWeek}"
                view.findViewById<TextView>(changeId).apply {
                    text = changeText
                    setTextColor(changeColor)
                    visibility = View.VISIBLE
                }
            }

            buildPainBars(
                view.findViewById(R.id.painBarsContainer),
                view.findViewById(R.id.painLabelsContainer),
                pain.daily
            )
            buildPainBars(
                view.findViewById(R.id.painBarsContainer2),
                view.findViewById(R.id.painLabelsContainer2),
                pain.daily
            )
        }

        data.weeklySummary?.let { summary ->
            view.findViewById<TextView>(R.id.tvSummarySession).text = "${summary.avgSessionMinutes} min"
            view.findViewById<TextView>(R.id.tvSummaryDays).text = "${summary.activeDays} days"
            val romGain = summary.romGainDegrees
            view.findViewById<TextView>(R.id.tvSummaryRom).text =
                if (romGain >= 0) "↑ ${romGain}°" else "↓ ${-romGain}°"
        }
    }

    private fun buildWeekDays(container: LinearLayout, days: List<DayStatus>) {
        container.removeAllViews()
        val dp = resources.displayMetrics.density
        val boxSize = (28 * dp).toInt()
        val boxMarginEnd = (4 * dp).toInt()
        val labelMarginEnd = (2 * dp).toInt()

        for (day in days) {
            val label = TextView(requireContext()).apply {
                text = day.day
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextLight))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = labelMarginEnd }
            }
            container.addView(label)

            val box = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(boxSize, boxSize).apply {
                    marginEnd = boxMarginEnd
                }
                background = ContextCompat.getDrawable(
                    requireContext(),
                    when {
                        day.isToday -> R.drawable.bg_day_upcoming
                        day.done -> R.drawable.bg_day_done
                        else -> R.drawable.bg_day_missed
                    }
                )
            }
            container.addView(box)
        }
    }

    private fun buildPainBars(
        barsContainer: LinearLayout,
        labelsContainer: LinearLayout,
        daily: List<PainPoint>
    ) {
        barsContainer.removeAllViews()
        labelsContainer.removeAllViews()
        val dp = resources.displayMetrics.density
        val maxLevel = 10
        val containerH = (80 * dp).toInt()
        val cornerRadius = 4 * dp
        val colMarginEnd = (3 * dp).toInt()

        for (point in daily) {
            val level = point.level.coerceIn(0, maxLevel)
            val barH = ((level.toFloat() / maxLevel) * containerH).toInt().coerceAtLeast(4)
            val barColor = when {
                level <= 3 -> Color.parseColor("#1D9E75")
                level <= 6 -> Color.parseColor("#EF9F27")
                else -> Color.parseColor("#E8745A")
            }

            val col = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    .apply { marginEnd = colMarginEnd }
            }

            val valLabel = TextView(requireContext()).apply {
                text = "$level"
                textSize = 10f
                setTextColor(when {
                    level <= 3 -> Color.parseColor("#1D9E75")
                    level <= 6 -> Color.parseColor("#BA7517")
                    else -> Color.parseColor("#9EB5AF")
                })
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            col.addView(valLabel)

            val bar = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barH)
                background = GradientDrawable().apply {
                    setColor(barColor)
                    cornerRadii = floatArrayOf(
                        cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0f, 0f, 0f, 0f
                    )
                }
            }
            col.addView(bar)
            barsContainer.addView(col)

            val xLabel = TextView(requireContext()).apply {
                text = point.date.takeLast(5)
                textSize = 10f
                setTextColor(Color.parseColor("#9EB5AF"))
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            labelsContainer.addView(xLabel)
        }
    }
}
