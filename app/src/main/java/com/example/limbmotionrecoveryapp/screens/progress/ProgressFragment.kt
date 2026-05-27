package com.example.limbmotionrecoveryapp.screens.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.limbmotionrecoveryapp.R

class ProgressFragment : Fragment() {

    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar)
        val contentGroup = view.findViewById<ViewGroup>(R.id.content_group)
        val errorGroup = view.findViewById<View>(R.id.error_group)
        val tvError = view.findViewById<TextView>(R.id.tv_error)
        val btnRetry = view.findViewById<View>(R.id.btn_retry)

        fun showLoading() {
            progressBar?.visibility = View.VISIBLE
            contentGroup?.visibility = View.GONE
            errorGroup?.visibility = View.GONE
        }

        fun showContent() {
            progressBar?.visibility = View.GONE
            contentGroup?.visibility = View.VISIBLE
            errorGroup?.visibility = View.GONE
        }

        fun showError(msg: String) {
            progressBar?.visibility = View.GONE
            contentGroup?.visibility = View.GONE
            errorGroup?.visibility = View.VISIBLE
            tvError?.text = msg
        }

        btnRetry?.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
            val token = prefs.getString("token", "") ?: ""
            val userId = prefs.getInt("userId", 0)
            if (token.isNotBlank()) {
                viewModel.load(userId, token)
            }
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is ProgressViewModel.State.Loading -> showLoading()
                is ProgressViewModel.State.Success -> {
                    showContent()
                    bindData(view, state.data)
                }
                is ProgressViewModel.State.Error -> showError(state.message)
            }
        })

        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val userId = prefs.getInt("userId", 0)
        if (token.isNotBlank()) {
            viewModel.load(userId, token)
        }
    }

    private fun bindData(view: View, data: ProgressData) {
        // 总训练次数 / 完成次数
        val completed = data.adherence?.completedExercises ?: 0
        view.findViewById<TextView>(R.id.tv_total_sessions)?.text = completed.toString()
        view.findViewById<TextView>(R.id.tv_completed_sessions)?.text = completed.toString()

        // 总时长（分钟）
        val avgMin = data.weeklySummary?.avgSessionMinutes ?: 0
        view.findViewById<TextView>(R.id.tv_total_duration)?.text = String.format("%.1f min", avgMin.toFloat())

        // 准确率环形图（adherence 百分比）
        val donut = view.findViewById<com.example.limbmotionrecoveryapp.screens.progress.charts.DonutChartView>(
            R.id.donut_chart_accuracy
        )
        donut?.setPercentage(data.adherence?.weeklyPercent ?: 0)

        // 折线图：传入 ROM 和 Pain 数据
        val lineChart = view.findViewById<com.example.limbmotionrecoveryapp.screens.progress.charts.LineChartView>(
            R.id.line_chart
        )
        lineChart?.setData(data.rom?.history ?: emptyList(), data.pain?.daily ?: emptyList())
    }
}
