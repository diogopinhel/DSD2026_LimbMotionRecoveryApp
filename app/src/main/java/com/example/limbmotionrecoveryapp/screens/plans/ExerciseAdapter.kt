package com.example.limbmotionrecoveryapp.screens.plans

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.limbmotionrecoveryapp.R

class ExerciseAdapter(
    private val onExerciseClick: (Exercise) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class ListItem {
        data class SectionHeader(val phase: String) : ListItem()
        data class ExerciseEntry(val exercise: Exercise) : ListItem()
    }

    private var items: List<ListItem> = emptyList()

    fun submitExercises(exercises: List<Exercise>) {
        val newItems = mutableListOf<ListItem>()
        exercises.groupBy { it.phase }.forEach { (phase, exList) ->
            newItems += ListItem.SectionHeader(phase)
            newItems += exList.map { ListItem.ExerciseEntry(it) }
        }
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ListItem.SectionHeader -> 0
        is ListItem.ExerciseEntry -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            HeaderVH(inflater.inflate(R.layout.item_exercise_section_header, parent, false))
        } else {
            ExVH(inflater.inflate(R.layout.item_exercise_row, parent, false))
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.SectionHeader -> (holder as HeaderVH).bind(item.phase)
            is ListItem.ExerciseEntry -> (holder as ExVH).bind(item.exercise, onExerciseClick)
        }
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvPhase: TextView = view.findViewById(R.id.tvPhaseLabel)
        fun bind(phase: String) {
            tvPhase.text = phase
            tvPhase.setTextColor(when (phase.lowercase()) {
                "warm up", "warmup" -> Color.parseColor("#BA7517")
                "strength" -> Color.parseColor("#1D9E75")
                "cooldown", "cool down" -> Color.parseColor("#378ADD")
                else -> Color.parseColor("#9EB5AF")
            })
        }
    }

    class ExVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvExName)
        private val tvMeta: TextView = view.findViewById(R.id.tvExMeta)
        private val tvPain: TextView = view.findViewById(R.id.tvPainBadge)
        private val tvVideo: TextView = view.findViewById(R.id.tvVideoBadge)

        fun bind(ex: Exercise, onClick: (Exercise) -> Unit) {
            tvName.text = ex.name
            tvMeta.text = ex.metaText
            itemView.alpha = if (ex.completed) 0.55f else 1f

            tvVideo.visibility = if (!ex.gifUrl.isNullOrBlank()) View.VISIBLE else View.GONE

            if (ex.lastPainLevel != null) {
                tvPain.visibility = View.VISIBLE
                tvPain.text = "Pain ${ex.lastPainLevel} last time"
            } else {
                tvPain.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(ex) }
        }
    }
}
