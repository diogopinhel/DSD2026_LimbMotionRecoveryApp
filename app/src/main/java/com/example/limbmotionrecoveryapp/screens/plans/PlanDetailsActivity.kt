package com.example.limbmotionrecoveryapp.screens.plans

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.button.MaterialButton

class PlanDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAN_ID = "plan_id"
        const val EXTRA_PLAN_NAME = "plan_name"
        const val EXTRA_PLAN_STATUS = "plan_status"
        const val EXTRA_DOCTOR = "doctor_name"
        const val EXTRA_PROGRESS = "progress_percent"
        const val EXTRA_COMPLETED_SESSIONS = "completed_sessions"
        const val EXTRA_TOTAL_SESSIONS = "total_sessions"
        const val EXTRA_START_DATE = "start_date"
        const val EXTRA_END_DATE = "end_date"
    }

    private val viewModel: PlanDetailsViewModel by viewModels()
    private lateinit var adapter: ExerciseAdapter
    private var showingTodo = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_details)

        val planId = intent.getIntExtra(EXTRA_PLAN_ID, -1)
        val planName = intent.getStringExtra(EXTRA_PLAN_NAME) ?: ""
        val status = intent.getStringExtra(EXTRA_PLAN_STATUS) ?: ""
        val doctor = intent.getStringExtra(EXTRA_DOCTOR) ?: ""
        val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
        val completedSessions = intent.getIntExtra(EXTRA_COMPLETED_SESSIONS, 0)
        val totalSessions = intent.getIntExtra(EXTRA_TOTAL_SESSIONS, 0)
        val startDate = intent.getStringExtra(EXTRA_START_DATE) ?: ""
        val endDate = intent.getStringExtra(EXTRA_END_DATE) ?: ""

        val token = getSharedPreferences("auth", MODE_PRIVATE).getString("token", "") ?: ""

        setupViews(planName, status, doctor, progress, completedSessions, totalSessions, startDate, endDate)
        setupTabs()
        setupList()
        observeState()

        if (planId != -1 && token.isNotBlank()) {
            viewModel.load(planId, token)
        }
    }

    private fun setupViews(
        planName: String, status: String, doctor: String,
        progress: Int, completed: Int, total: Int,
        startDate: String, endDate: String
    ) {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        // Top bar
        val parts = planName.split("—").map { it.trim() }
        findViewById<TextView>(R.id.tvTopBarTitle).text = parts.firstOrNull() ?: planName
        val subParts = mutableListOf<String>()
        if (parts.size > 1) subParts.add(parts[1])
        if (doctor.isNotBlank()) subParts.add(doctor)
        findViewById<TextView>(R.id.tvTopBarSub).text = subParts.joinToString(" · ")
        findViewById<TextView>(R.id.tvStatusBadge).text = status.replaceFirstChar { it.uppercase() }

        // Hero
        findViewById<TextView>(R.id.tvProgressPct).text = "$progress%"
        findViewById<TextView>(R.id.tvSessionsCount).text = "$completed of $total sessions done"
        findViewById<ProgressBar>(R.id.heroProgressBar).progress = progress
        if (startDate.isNotBlank() && endDate.isNotBlank()) {
            findViewById<TextView>(R.id.tvStatDates).text = "$startDate – $endDate"
        }

        // Bottom bar sub (will update when exercises load)
        findViewById<TextView>(R.id.tvStartSub).text = "Loading exercises…"
        findViewById<MaterialButton>(R.id.btnStartSession).setOnClickListener {
            val intent = android.content.Intent(this, com.example.limbmotionrecoveryapp.screens.session.SessionPlayerActivity::class.java)
            val exerciseNames = arrayListOf<String>()
            startActivity(intent)
        }
    }

    private fun setupTabs() {
        val tabTodo = findViewById<TextView>(R.id.tabTodo)
        val tabCompleted = findViewById<TextView>(R.id.tabCompleted)
        val indicator = findViewById<View>(R.id.tabIndicator)

        fun selectTab(todo: Boolean) {
            showingTodo = todo
            val state = viewModel.state.value
            if (state is PlanDetailsViewModel.State.Success) {
                val list = if (todo) state.details.todoExercises else state.details.doneExercises
                adapter.submitExercises(list)
            }
            tabTodo.setTextColor(
                if (todo) android.graphics.Color.parseColor("#1D9E75")
                else android.graphics.Color.parseColor("#9EB5AF")
            )
            tabCompleted.setTextColor(
                if (!todo) android.graphics.Color.parseColor("#1D9E75")
                else android.graphics.Color.parseColor("#9EB5AF")
            )
            // Move indicator
            indicator.post {
                val parent = indicator.parent as FrameLayout
                val tabWidth = parent.width / 2
                val lp = indicator.layoutParams as FrameLayout.LayoutParams
                lp.width = tabWidth
                lp.marginStart = if (todo) 0 else tabWidth
                indicator.layoutParams = lp
            }
        }

        tabTodo.setOnClickListener { selectTab(true) }
        tabCompleted.setOnClickListener { selectTab(false) }
        selectTab(true)
    }

    private fun setupList() {
        val token = getSharedPreferences("auth", MODE_PRIVATE).getString("token", "") ?: ""
        adapter = ExerciseAdapter { exercise ->
            val sheet = ExerciseDetailSheet.newInstance(exercise)
            sheet.onMarkDone = { exerciseId, scheduleId ->
                viewModel.markDone(scheduleId, exerciseId, null, token)
            }
            sheet.show(supportFragmentManager, "exercise_detail")
        }
        val rv = findViewById<RecyclerView>(R.id.rvExercises)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun observeState() {
        val rv = findViewById<RecyclerView>(R.id.rvExercises)
        val layoutState = findViewById<LinearLayout>(R.id.layoutState)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvMessage = findViewById<TextView>(R.id.tvStateMessage)
        val tvStartSub = findViewById<TextView>(R.id.tvStartSub)

        viewModel.state.observe(this) { state ->
            when (state) {
                is PlanDetailsViewModel.State.Loading -> {
                    rv.visibility = View.GONE
                    layoutState.visibility = View.VISIBLE
                    progressBar.visibility = View.VISIBLE
                    tvMessage.visibility = View.GONE
                }
                is PlanDetailsViewModel.State.Success -> {
                    layoutState.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    val list = if (showingTodo) state.details.todoExercises else state.details.doneExercises
                    adapter.submitExercises(list)
                    val todo = state.details.todoExercises.size
                    val est = state.details.estimatedMinutes
                    tvStartSub.text = "$todo exercises${if (est > 0) " · ~${est} min" else ""}"
                    updateTabLabels(state.details)
                }
                is PlanDetailsViewModel.State.Error -> {
                    rv.visibility = View.GONE
                    layoutState.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    tvMessage.visibility = View.VISIBLE
                    tvMessage.text = state.message
                }
            }
        }
    }

    private fun updateTabLabels(details: PlanDetailsViewModel.PlanDetails) {
        findViewById<TextView>(R.id.tabTodo).text = "To Do (${details.todoExercises.size})"
        findViewById<TextView>(R.id.tabCompleted).text = "Completed (${details.doneExercises.size})"
    }
}
