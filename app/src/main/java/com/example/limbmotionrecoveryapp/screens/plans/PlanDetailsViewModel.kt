package com.example.limbmotionrecoveryapp.screens.plans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlanDetailsViewModel : ViewModel() {

    data class PlanDetails(
        val planName: String = "",
        val phaseDoctorSub: String = "",
        val status: String = "",
        val progressPercent: Int = 0,
        val completedSessions: Int = 0,
        val totalSessions: Int = 0,
        val startDate: String = "",
        val endDate: String = "",
        val estimatedMinutes: Int = 0,
        val exercises: List<Exercise> = emptyList()
    ) {
        val todoExercises get() = exercises.filter { !it.completed }
        val doneExercises get() = exercises.filter { it.completed }
    }

    sealed class State {
        object Loading : State()
        // ❌ V2 ENDPOINT MISSING — GET /schedule/{scheduleId}/exercises
        // See docs/V2_API_REQUIREMENTS.md — section 3 — Priority 🔴
        data class EndpointMissing(val message: String = "Exercise list endpoint not yet available.\nSee docs/V2_API_REQUIREMENTS.md") : State()
        data class Success(val details: PlanDetails) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    fun load(planId: Int, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // ❌ V2 ENDPOINT MISSING: GET /schedule/{planId}/exercises
            // When V2 provides this endpoint, replace the line below with the real API call:
            //
            //   val api = V2ApiClient()
            //   val response = api.getPlanExercises(planId, token)  // method to be added to V2ApiClient
            //   val details = parsePlanDetails(response)
            //   _state.postValue(State.Success(details))
            //
            _state.postValue(State.EndpointMissing())
        }
    }
}
