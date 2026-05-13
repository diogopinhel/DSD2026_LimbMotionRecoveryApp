package com.example.limbmotionrecoveryapp.screens.plans

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.limbmotionrecoveryapp.R

class PlanAdapter(
    private val onPlanClick: (Plan) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class ListItem {
        data class Header(val title: String) : ListItem()
        data class PlanEntry(val plan: Plan) : ListItem()
    }

    private var items: List<ListItem> = emptyList()

    fun submitPlans(plans: List<Plan>) {
        val newItems = mutableListOf<ListItem>()
        val active = plans.filter { it.isActive }
        val upcoming = plans.filter { it.isUpcoming }
        val completed = plans.filter { it.isCompleted }
        if (active.isNotEmpty()) {
            newItems += ListItem.Header("Active")
            newItems += active.map { ListItem.PlanEntry(it) }
        }
        if (upcoming.isNotEmpty()) {
            newItems += ListItem.Header("Upcoming")
            newItems += upcoming.map { ListItem.PlanEntry(it) }
        }
        if (completed.isNotEmpty()) {
            newItems += ListItem.Header("Completed")
            newItems += completed.map { ListItem.PlanEntry(it) }
        }
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ListItem.Header -> 0
        is ListItem.PlanEntry -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val view = inflater.inflate(R.layout.item_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_plan_card, parent, false)
            PlanViewHolder(view)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is ListItem.PlanEntry -> (holder as PlanViewHolder).bind(item.plan, onPlanClick)
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvSectionHeader)
        fun bind(title: String) { tvTitle.text = title }
    }

    class PlanViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardRoot: LinearLayout = view.findViewById(R.id.cardRoot)
        private val tvName: TextView = view.findViewById(R.id.tvPlanName)
        private val tvBadge: TextView = view.findViewById(R.id.tvBadge)
        private val tvDoctorInitials: TextView = view.findViewById(R.id.tvDoctorInitials)
        private val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
        private val tvDates: TextView = view.findViewById(R.id.tvDates)
        private val tagsContainer: LinearLayout = view.findViewById(R.id.tagsContainer)
        private val tvProgressPct: TextView = view.findViewById(R.id.tvProgressPct)
        private val tvSessionsCount: TextView = view.findViewById(R.id.tvSessionsCount)
        private val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        private val tvFooterStat: TextView = view.findViewById(R.id.tvFooterStat)
        private val tvFooterAction: TextView = view.findViewById(R.id.tvFooterAction)

        fun bind(plan: Plan, onClick: (Plan) -> Unit) {
            tvName.text = plan.name

            // Badge
            when {
                plan.isActive -> {
                    tvBadge.text = "Active"
                    tvBadge.setTextColor(Color.parseColor("#085041"))
                    tvBadge.setBackgroundResource(R.drawable.bg_badge)
                    cardRoot.setBackgroundResource(R.drawable.bg_plan_card_active)
                }
                plan.isUpcoming -> {
                    tvBadge.text = "Upcoming"
                    tvBadge.setTextColor(Color.parseColor("#0C447C"))
                    tvBadge.background = null
                    tvBadge.setBackgroundResource(R.drawable.bg_tag_blue)
                    cardRoot.setBackgroundResource(R.drawable.bg_plan_card_default)
                }
                else -> {
                    tvBadge.text = "Completed"
                    tvBadge.setTextColor(Color.parseColor("#5F5E5A"))
                    tvBadge.setBackgroundResource(R.drawable.bg_tag_gray)
                    cardRoot.setBackgroundResource(R.drawable.bg_plan_card_default)
                    cardRoot.alpha = 0.75f
                }
            }

            // Doctor
            val initials = plan.doctorName.split(" ")
                .filter { it.isNotBlank() }.takeLast(2)
                .joinToString("") { it.first().uppercase() }
            tvDoctorInitials.text = initials
            tvDoctorName.text = plan.doctorName

            // Dates
            tvDates.text = if (plan.startDate.isNotEmpty() && plan.endDate.isNotEmpty())
                "${plan.startDate} – ${plan.endDate}" else ""

            // Phase tags
            tagsContainer.removeAllViews()
            plan.phases.forEach { phase ->
                val tag = TextView(tagsContainer.context).apply {
                    text = phase
                    textSize = 11f
                    setTextColor(when {
                        plan.isActive -> Color.parseColor("#0F6E56")
                        plan.isUpcoming -> Color.parseColor("#185FA5")
                        else -> Color.parseColor("#5F5E5A")
                    })
                    setBackgroundResource(when {
                        plan.isActive -> R.drawable.bg_tag_green
                        plan.isUpcoming -> R.drawable.bg_tag_blue
                        else -> R.drawable.bg_tag_gray
                    })
                    setPadding(16, 6, 16, 6)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.marginEnd = 12
                    layoutParams = lp
                }
                tagsContainer.addView(tag)
            }

            // Progress
            progressBar.progress = plan.progressPercent
            when {
                plan.isActive -> {
                    tvProgressPct.text = "${plan.progressPercent}% complete"
                    tvProgressPct.setTextColor(Color.parseColor("#1D9E75"))
                    tvSessionsCount.text = "${plan.completedSessions} of ${plan.totalSessions} sessions"
                }
                plan.isUpcoming -> {
                    tvProgressPct.text = "Starts ${plan.startDate}"
                    tvProgressPct.setTextColor(Color.parseColor("#378ADD"))
                    tvSessionsCount.text = "${plan.totalSessions} sessions"
                }
                else -> {
                    tvProgressPct.text = "100% complete"
                    tvProgressPct.setTextColor(Color.parseColor("#888780"))
                    tvSessionsCount.text = "${plan.totalSessions} of ${plan.totalSessions} sessions"
                }
            }

            // Footer
            when {
                plan.isActive -> {
                    val today = if (plan.todayExercises > 0) "Today: ${plan.todayExercises} exercises" else "Today: active"
                    tvFooterStat.text = today
                    tvFooterStat.setTextColor(Color.parseColor("#1D9E75"))
                    tvFooterAction.text = "Open plan →"
                    tvFooterAction.setTextColor(Color.parseColor("#1D9E75"))
                }
                plan.isUpcoming -> {
                    tvFooterStat.text = "Preview plan"
                    tvFooterStat.setTextColor(Color.parseColor("#9EB5AF"))
                    tvFooterAction.text = "View details →"
                    tvFooterAction.setTextColor(Color.parseColor("#9EB5AF"))
                }
                else -> {
                    tvFooterStat.text = "All sessions done"
                    tvFooterStat.setTextColor(Color.parseColor("#9EB5AF"))
                    tvFooterAction.text = "View summary →"
                    tvFooterAction.setTextColor(Color.parseColor("#9EB5AF"))
                }
            }

            cardRoot.setOnClickListener { onClick(plan) }
        }
    }
}
