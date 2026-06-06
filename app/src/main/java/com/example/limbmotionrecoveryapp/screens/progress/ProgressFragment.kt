package com.example.limbmotionrecoveryapp.screens.progress

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dsd.m1.api.V2ApiClient
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgressFragment : Fragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var contentGroup: View
    private lateinit var emptyGroup: View
    private lateinit var errorGroup: View
    private lateinit var tvError: TextView
    private lateinit var btnRetry: MaterialButton
    private lateinit var btnRefresh: MaterialButton

    private val adapter = HistoryAdapter(
        onItemClick = { session, holder -> toggleDetail(session, holder) },
        onDeleteClick = { session -> confirmDelete(session) }
    )

    private val apiClient = V2ApiClient()
    private var token: String = ""
    private var userId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadCredentials()
        setupRecyclerView()
        setupListeners()

        if (token.isNotBlank() && userId != 0) {
            loadHistory()
        } else {
            showError("Not logged in. Please sign in first.")
        }
    }

    private fun initViews(view: View) {
        rvHistory = view.findViewById(R.id.rvHistory)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progress_bar)
        contentGroup = view.findViewById(R.id.content_group)
        emptyGroup = view.findViewById(R.id.empty_group)
        errorGroup = view.findViewById(R.id.error_group)
        tvError = view.findViewById(R.id.tv_error)
        btnRetry = view.findViewById(R.id.btn_retry)
        btnRefresh = view.findViewById(R.id.btnRefresh)
    }

    private fun loadCredentials() {
        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        token = prefs.getString("token", "") ?: ""
        userId = prefs.getInt("userId", 0)
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter
    }

    private fun setupListeners() {
        swipeRefresh.setOnRefreshListener { loadHistory() }
        btnRetry.setOnClickListener { loadHistory() }
        btnRefresh.setOnClickListener { loadHistory() }
    }

    /**
     * UC-M1-03-01 Refresh Historical Records
     */
    private fun loadHistory() {
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sessions = withContext(Dispatchers.IO) {
                    apiClient.getSessions(userId = userId, token = token)
                }
                swipeRefresh.isRefreshing = false
                if (sessions.isEmpty()) {
                    showEmpty()
                } else {
                    adapter.submitData(sessions)
                    showContent()
                }
            } catch (e: Exception) {
                swipeRefresh.isRefreshing = false
                showError(e.message ?: "Failed to load history. Please check your network.")
            }
        }
    }

    /**
     * UC-M1-03-02 Delete Historical Records
     */
    private fun confirmDelete(session: Map<String, Any?>) {
        val sessionId = (session["id"] as? Number)?.toInt() ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Record")
            .setMessage("Are you sure you want to delete this session? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteSession(sessionId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSession(sessionId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    apiClient.deleteSession(sessionId, token)
                }
                Snackbar.make(requireView(), "Record deleted successfully", Snackbar.LENGTH_SHORT).show()
                loadHistory()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * UC-M1-03-03 Browse + UC-M1-03-04 AI Evaluation
     */
    private fun toggleDetail(session: Map<String, Any?>, holder: HistoryAdapter.ViewHolder) {
        val sessionId = (session["id"] as? Number)?.toInt() ?: return
        val isExpanded = holder.detailGroup.visibility == View.VISIBLE

        if (isExpanded) {
            holder.detailGroup.visibility = View.GONE
            return
        }

        holder.detailGroup.visibility = View.VISIBLE
        holder.tvAiEvaluation.text = "Loading AI evaluation..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val recommendations = withContext(Dispatchers.IO) {
                    apiClient.getSessionRecommendations(sessionId, token)
                }
                holder.tvAiEvaluation.text = if (recommendations.isEmpty()) {
                    "No AI evaluation available for this session."
                } else {
                    recommendations.joinToString("\n\n") { rec ->
                        val movement = rec["movement"]?.toString() ?: "Unknown"
                        val confidence = rec["confidence"]?.toString() ?: "N/A"
                        val notes = rec["notes"]?.toString()
                        buildString {
                            append("• Movement: $movement")
                            append("\n  Confidence: $confidence")
                            if (!notes.isNullOrBlank()) append("\n  Notes: $notes")
                        }
                    }
                }
            } catch (e: Exception) {
                holder.tvAiEvaluation.text = "Failed to load AI evaluation: ${e.message}"
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        contentGroup.visibility = View.GONE
        emptyGroup.visibility = View.GONE
        errorGroup.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        contentGroup.visibility = View.VISIBLE
        emptyGroup.visibility = View.GONE
        errorGroup.visibility = View.GONE
    }

    private fun showEmpty() {
        progressBar.visibility = View.GONE
        contentGroup.visibility = View.GONE
        emptyGroup.visibility = View.VISIBLE
        errorGroup.visibility = View.GONE
    }

    private fun showError(msg: String) {
        progressBar.visibility = View.GONE
        contentGroup.visibility = View.GONE
        emptyGroup.visibility = View.GONE
        errorGroup.visibility = View.VISIBLE
        tvError.text = msg
    }
}