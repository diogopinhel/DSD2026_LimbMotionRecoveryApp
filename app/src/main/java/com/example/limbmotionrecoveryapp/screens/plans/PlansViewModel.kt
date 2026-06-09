package com.example.limbmotionrecoveryapp.screens.plans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.m1.api.V2ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlansViewModel : ViewModel() {

    private val api = V2ApiClient()

    data class PlansState(
        val plans: List<Plan> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null
    ) {
        val activePlans get() = plans.count { it.isActive }
        val totalSessions get() = plans.sumOf { it.totalSessions }
        val overallPercent: Int get() {
            val total = totalSessions
            return if (total > 0) plans.sumOf { it.completedSessions } * 100 / total else 0
        }
    }

    private val _state = MutableLiveData(PlansState())
    val state: LiveData<PlansState> = _state

    fun load(userId: Int, token: String) {
        if (_state.value?.loading == true) return
        _state.value = PlansState(loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val raw = api.getSchedule(userId, token)
                val plans = raw.mapNotNull { item -> parsePlan(item) }
                _state.postValue(PlansState(plans = plans.ifEmpty { demoPlans() }))
            } catch (e: Exception) {
                _state.postValue(PlansState(plans = demoPlans()))
            }
        }
    }

    private fun demoPlans(): List<Plan> = listOf(
        Plan(
            id = 1,
            name = "Knee Recovery — Phase 1",
            status = "active",
            startDate = "Jun 1",
            endDate = "Jul 15",
            totalSessions = 24,
            completedSessions = 8,
            phases = listOf("Phase 1", "Strength"),
            doctorName = "Dr. Silva",
            todayExercises = 4
        ),
        Plan(
            id = 2,
            name = "Post-Op Mobility",
            status = "upcoming",
            startDate = "Jul 20",
            endDate = "Aug 30",
            totalSessions = 12,
            completedSessions = 0,
            phases = listOf("Phase 2"),
            doctorName = "Dr. Silva",
            todayExercises = 0
        )
    )

    @Suppress("UNCHECKED_CAST")
    private fun parsePlan(item: Map<String, Any?>): Plan? {
        return try {
            Plan(
                id = (item["id"] as? Double)?.toInt() ?: return null,
                name = item["name"] as? String ?: item["title"] as? String
                    ?: item["exercise"] as? String ?: "Unnamed plan",
                status = item["status"] as? String ?: "pending",
                startDate = item["startDate"] as? String ?: item["start_date"] as? String
                    ?: item["date"] as? String ?: "",
                endDate = item["endDate"] as? String ?: item["end_date"] as? String ?: "",
                totalSessions = (item["totalSessions"] as? Double)?.toInt()
                    ?: (item["total_sessions"] as? Double)?.toInt() ?: 0,
                completedSessions = (item["completedSessions"] as? Double)?.toInt()
                    ?: (item["completed_sessions"] as? Double)?.toInt() ?: 0,
                phases = (item["phases"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                doctorName = item["doctorName"] as? String ?: item["doctor_name"] as? String ?: "",
                todayExercises = (item["todayExercises"] as? Double)?.toInt()
                    ?: (item["today_exercises"] as? Double)?.toInt() ?: 0
            )
        } catch (_: Exception) { null }
    }
}
