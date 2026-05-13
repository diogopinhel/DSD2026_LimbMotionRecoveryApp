package com.example.limbmotionrecoveryapp.screens.plans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.limbmotionrecoveryapp.R

class PlansFragment : Fragment() {

    private val viewModel: PlansViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_plans, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val userId = prefs.getInt("userId", 0)
        val userName = prefs.getString("userName", "") ?: ""

        val tvHeaderSub = view.findViewById<TextView>(R.id.tvHeaderSub)
        val tvActivePlans = view.findViewById<TextView>(R.id.tvActivePlans)
        val tvTotalSessions = view.findViewById<TextView>(R.id.tvTotalSessions)
        val tvOverallPct = view.findViewById<TextView>(R.id.tvOverallPct)
        val rvPlans = view.findViewById<RecyclerView>(R.id.rvPlans)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val adapter = PlanAdapter { plan ->
            val intent = android.content.Intent(requireContext(), PlanDetailsActivity::class.java).apply {
                putExtra(PlanDetailsActivity.EXTRA_PLAN_ID, plan.id)
                putExtra(PlanDetailsActivity.EXTRA_PLAN_NAME, plan.name)
                putExtra(PlanDetailsActivity.EXTRA_PLAN_STATUS, plan.status)
                putExtra(PlanDetailsActivity.EXTRA_DOCTOR, plan.doctorName)
                putExtra(PlanDetailsActivity.EXTRA_PROGRESS, plan.progressPercent)
                putExtra(PlanDetailsActivity.EXTRA_COMPLETED_SESSIONS, plan.completedSessions)
                putExtra(PlanDetailsActivity.EXTRA_TOTAL_SESSIONS, plan.totalSessions)
                putExtra(PlanDetailsActivity.EXTRA_START_DATE, plan.startDate)
                putExtra(PlanDetailsActivity.EXTRA_END_DATE, plan.endDate)
            }
            startActivity(intent)
        }

        rvPlans.layoutManager = LinearLayoutManager(requireContext())
        rvPlans.adapter = adapter

        viewModel.state.observe(viewLifecycleOwner) { state ->
            progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE
            rvPlans.visibility = if (state.loading) View.GONE else View.VISIBLE

            if (state.error != null) {
                tvEmpty.visibility = View.VISIBLE
                tvEmpty.text = state.error
                rvPlans.visibility = View.GONE
            } else if (!state.loading) {
                tvActivePlans.text = state.activePlans.toString()
                tvTotalSessions.text = state.totalSessions.toString()
                tvOverallPct.text = "${state.overallPercent}%"

                if (state.plans.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvPlans.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    adapter.submitPlans(state.plans)
                    val activePlan = state.plans.firstOrNull { it.isActive }
                    tvHeaderSub.text = if (activePlan != null) activePlan.name else if (userName.isNotBlank()) "Hello, $userName" else ""
                }
            }
        }

        if (token.isNotBlank() && userId > 0) {
            viewModel.load(userId, token)
        } else {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Please log in again"
            rvPlans.visibility = View.GONE
        }
    }
}
