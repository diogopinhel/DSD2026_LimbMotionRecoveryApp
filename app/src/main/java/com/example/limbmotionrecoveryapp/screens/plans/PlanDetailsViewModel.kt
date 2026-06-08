package com.example.limbmotionrecoveryapp.screens.plans

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.m1.api.V2ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlanDetailsViewModel : ViewModel() {

    private val api = V2ApiClient()

    data class PlanDetails(
        val estimatedMinutes: Int = 0,
        val exercises: List<Exercise> = emptyList()
    ) {
        val todoExercises get() = exercises.filter { !it.completed }
        val doneExercises get() = exercises.filter { it.completed }
    }

    sealed class State {
        object Loading : State()
        data class Success(val details: PlanDetails) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    fun load(planId: Int, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.postValue(State.Loading)
            try {
                val response = api.getScheduleExercises(planId, token)
                val exercises = parseExercises(planId, response)
                val estimatedMinutes = exercises.sumOf { it.sets * it.reps * 30 } / 60
                _state.postValue(State.Success(PlanDetails(estimatedMinutes, exercises)))
            } catch (e: Exception) {
                _state.postValue(State.Error(e.message ?: "Failed to load exercises"))
            }
        }
    }

    fun markDone(scheduleId: Int, exerciseId: Int, painLevel: Int?, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                api.completeScheduleExercise(scheduleId, exerciseId, painLevel, token)
                val current = _state.value
                if (current is State.Success) {
                    val updated = current.details.exercises.map { ex ->
                        if (ex.id == exerciseId) ex.copy(completed = true, lastPainLevel = painLevel) else ex
                    }
                    _state.postValue(State.Success(current.details.copy(exercises = updated)))
                }
            } catch (_: Exception) {
                // silently ignore — UI shows optimistic update only on success
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseExercises(planId: Int, response: Map<String, Any?>): List<Exercise> {
        // V2 may return { "exercises": [...] } or the list directly at top level
        val rawList: List<Map<String, Any?>> = when {
            response.containsKey("exercises") ->
                (response["exercises"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            response.containsKey("data") ->
                (response["data"] as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
            else -> emptyList()
        }

        return rawList.map { item ->
            Exercise(
                id = (item["id"] as? Double)?.toInt() ?: 0,
                scheduleId = planId,
                name = item["name"] as? String ?: "",
                phase = item["phase"] as? String ?: "General",
                sets = (item["sets"] as? Double)?.toInt() ?: 1,
                reps = (item["reps"] as? Double)?.toInt() ?: 1,
                holdSeconds = ((item["holdSeconds"] ?: item["hold_seconds"]) as? Double)?.toInt() ?: 0,
                notes = item["notes"] as? String,
                gifUrl = (item["gif_url"] ?: item["gifUrl"]) as? String,
                description = item["description"] as? String,
                completed = item["completed"] as? Boolean ?: false,
                lastPainLevel = ((item["lastPainLevel"] ?: item["last_pain_level"]) as? Double)?.toInt()
            )
        }
    }
}
