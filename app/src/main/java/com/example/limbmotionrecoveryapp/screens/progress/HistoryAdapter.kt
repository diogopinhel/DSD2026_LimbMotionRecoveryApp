package com.example.limbmotionrecoveryapp.screens.progress

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (Map<String, Any?>, ViewHolder) -> Unit,
    private val onDeleteClick: (Map<String, Any?>) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var data: List<Map<String, Any?>> = emptyList()

    fun submitData(list: List<Map<String, Any?>>) {
        data = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        val detailGroup: LinearLayout = itemView.findViewById(R.id.detailGroup)
        val tvAiEvaluation: TextView = itemView.findViewById(R.id.tvAiEvaluation)

        fun bind(session: Map<String, Any?>) {
            Log.d("HistoryAdapter", "Session raw: $session")

            val rawDate = session["started_at"]?.toString()
            val endedAt = session["ended_at"]?.toString()
            val userName = session["user_name"]?.toString() ?: "Unknown"

            tvDate.text = if (rawDate != null && rawDate != "null") {
                formatDate(rawDate)
            } else {
                "Unknown"
            }

            tvDuration.text = if (endedAt != null && endedAt != "null") {
                "Session ended"
            } else {
                "In progress"
            }

            // 状态判断：ended_at 非 null 即 completed
            val isCompleted = endedAt != null && endedAt != "null"
            tvStatus.text = if (isCompleted) "Completed" else "In Progress"

            val statusColor = if (isCompleted) {
                itemView.context.getColor(R.color.colorPrimaryGreen)
            } else {
                itemView.context.getColor(R.color.colorTextMedium)
            }
            tvStatus.setTextColor(statusColor)

            itemView.setOnClickListener { onItemClick(session, this) }
            btnDelete.setOnClickListener { onDeleteClick(session) }
        }

        private fun formatDate(raw: String): String {
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                formatter.format(parser.parse(raw) ?: return raw.take(10))
            } catch (_: Exception) {
                raw.take(10)
            }
        }
    }
}