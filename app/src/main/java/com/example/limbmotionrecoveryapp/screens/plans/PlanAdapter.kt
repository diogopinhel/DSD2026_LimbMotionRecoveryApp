package com.example.limbmotionrecoveryapp.screens.plans

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.limbmotionrecoveryapp.R
import java.text.SimpleDateFormat
import java.util.*

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
        private val rowDoctor: LinearLayout = view.findViewById(R.id.rowDoctor)
        private val tvDoctorInitials: TextView = view.findViewById(R.id.tvDoctorInitials)
        private val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
        private val tvDates: TextView = view.findViewById(R.id.tvDates)
        private val tagsContainer: LinearLayout = view.findViewById(R.id.tagsContainer)
        private val tvProgressPct: TextView = view.findViewById(R.id.tvProgressPct)
        private val tvSessionsCount: TextView = view.findViewById(R.id.tvSessionsCount)
        private val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        private val cardFooter: LinearLayout = view.findViewById(R.id.cardFooter)
        private val tvFooterStat: TextView = view.findViewById(R.id.tvFooterStat)
        private val tvFooterAction: TextView = view.findViewById(R.id.tvFooterAction)

        fun bind(plan: Plan, onClick: (Plan) -> Unit) {
            tvName.text = plan.name
            cardRoot.alpha = if (plan.isCompleted) 0.75f else 1f

            // Card background
            cardRoot.setBackgroundResource(
                if (plan.isActive) R.drawable.bg_plan_card_active else R.drawable.bg_plan_card_default
            )

            // Badge
            when {
                plan.isActive -> {
                    tvBadge.text = "Active"
                    tvBadge.setTextColor(Color.parseColor("#085041"))
                    tvBadge.setBackgroundResource(R.drawable.bg_badge)
                }
                plan.isUpcoming -> {
                    tvBadge.text = "Upcoming"
                    tvBadge.setTextColor(Color.parseColor("#0C447C"))
                    tvBadge.setBackgroundResource(R.drawable.bg_tag_blue)
                }
                else -> {
                    tvBadge.text = "Completed"
                    tvBadge.setTextColor(Color.parseColor("#5F5E5A"))
                    tvBadge.setBackgroundResource(R.drawable.bg_tag_gray)
                }
            }

            // Doctor row — hide when no doctor assigned
            if (plan.doctorName.isBlank()) {
                rowDoctor.visibility = View.GONE
            } else {
                rowDoctor.visibility = View.VISIBLE
                val initials = plan.doctorName.split(" ")
                    .filter { it.isNotBlank() }.takeLast(2)
                    .joinToString("") { it.first().uppercase() }
                tvDoctorInitials.text = initials
                tvDoctorName.text = plan.doctorName

                val avatarColor = when {
                    plan.isActive -> Color.parseColor("#E1F5EE")
                    plan.isUpcoming -> Color.parseColor("#E6F1FB")
                    else -> Color.parseColor("#F1EFE8")
                }
                val avatarDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(avatarColor)
                }
                tvDoctorInitials.background = avatarDrawable
                tvDoctorInitials.setTextColor(when {
                    plan.isActive -> Color.parseColor("#085041")
                    plan.isUpcoming -> Color.parseColor("#0C447C")
                    else -> Color.parseColor("#5F5E5A")
                })
            }

            // Dates — hide when empty, format ISO → "Apr 4 – May 15"
            val startFmt = formatDate(plan.startDate)
            val endFmt = formatDate(plan.endDate)
            if (startFmt.isNotEmpty() && endFmt.isNotEmpty()) {
                tvDates.visibility = View.VISIBLE
                tvDates.text = "$startFmt – $endFmt"
            } else {
                tvDates.visibility = View.GONE
            }

            // Phase tags — hide container when empty
            tagsContainer.removeAllViews()
            if (plan.phases.isEmpty()) {
                tagsContainer.visibility = View.GONE
            } else {
                tagsContainer.visibility = View.VISIBLE
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
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { marginEnd = 12 }
                    }
                    tagsContainer.addView(tag)
                }
            }

            // Progress
            progressBar.progress = plan.progressPercent
            when {
                plan.isActive -> {
                    tvProgressPct.text = "${plan.progressPercent}% complete"
                    tvProgressPct.setTextColor(Color.parseColor("#1D9E75"))
                    tvSessionsCount.text = if (plan.totalSessions > 0)
                        "${plan.completedSessions} of ${plan.totalSessions} sessions"
                    else "— sessions"
                    progressBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#1D9E75"))
                    progressBar.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F5EF"))
                }
                plan.isUpcoming -> {
                    tvProgressPct.text = if (startFmt.isNotEmpty()) "Starts $startFmt" else "Upcoming"
                    tvProgressPct.setTextColor(Color.parseColor("#378ADD"))
                    tvSessionsCount.text = if (plan.totalSessions > 0)
                        "${plan.totalSessions} sessions"
                    else "— sessions"
                    progressBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#85B7EB"))
                    progressBar.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#E6F1FB"))
                }
                else -> {
                    tvProgressPct.text = "100% complete"
                    tvProgressPct.setTextColor(Color.parseColor("#888780"))
                    tvSessionsCount.text = if (plan.totalSessions > 0)
                        "${plan.totalSessions} of ${plan.totalSessions} sessions"
                    else "— sessions"
                    progressBar.progress = 100
                    progressBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#B4B2A9"))
                    progressBar.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8E8E4"))
                }
            }

            // Footer background — rounded bottom corners to match the card outline
            cardFooter.setBackgroundResource(
                if (plan.isActive) R.drawable.bg_plan_footer_active else R.drawable.bg_plan_footer_default
            )

            // Footer stat + action
            when {
                plan.isActive -> {
                    if (plan.todayExercises > 0) {
                        tvFooterStat.text = "Today: ${plan.todayExercises} exercises"
                        tvFooterStat.setTextColor(Color.parseColor("#1D9E75"))
                    } else {
                        tvFooterStat.text = "No exercises today"
                        tvFooterStat.setTextColor(Color.parseColor("#9EB5AF"))
                    }
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

        private fun formatDate(iso: String): String {
            if (iso.isBlank()) return ""
            return try {
                val src = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dst = SimpleDateFormat("MMM d", Locale.getDefault())
                dst.format(src.parse(iso)!!)
            } catch (_: Exception) { iso }
        }
    }
}
