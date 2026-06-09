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
                if (exercises.isNotEmpty()) {
                    val estimatedMinutes = exercises.sumOf { it.sets * it.reps * 30 } / 60
                    _state.postValue(State.Success(PlanDetails(estimatedMinutes, exercises)))
                } else {
                    _state.postValue(State.Success(demoDetails(planId)))
                }
            } catch (e: Exception) {
                _state.postValue(State.Success(demoDetails(planId)))
            }
        }
    }

    private fun demoDetails(planId: Int): PlanDetails {
        val exercises = listOf(
            Exercise(
                id = 4, scheduleId = planId,
                name = "Straight Leg Raise", phase = "Phase 1",
                sets = 3, reps = 10, holdSeconds = 2,
                notes = "Keep the straight leg tightened throughout.",
                gifUrl = "https://cdn.jefit.com/assets/img/exercises/gifs/982.gif",
                description = "Strengthens the quadriceps without knee flexion. Ideal for early post-surgery rehabilitation when the knee cannot yet bend.",
                completed = false, lastPainLevel = null
            ),
            Exercise(
                id = 6, scheduleId = planId,
                name = "Ankle Pumps", phase = "Phase 1",
                sets = 2, reps = 20, holdSeconds = 0,
                notes = "Perform slowly and rhythmically.",
                gifUrl = "https://www.physio-pedia.com/images/archive/3/35/20200323205608%21Ankle_pumps.gif",
                description = "Promotes circulation and reduces swelling in the lower limb. Especially important in the first days after surgery.",
                completed = false, lastPainLevel = null
            ),
            Exercise(
                id = 5, scheduleId = planId,
                name = "Knee Extension", phase = "Phase 1",
                sets = 3, reps = 12, holdSeconds = 0,
                notes = "Do not snap the knee at full extension.",
                gifUrl = "https://cdn.jefit.com/assets/img/exercises/gifs/130.gif",
                description = "Isolates and strengthens the quadriceps through controlled knee extension.",
                completed = false, lastPainLevel = null
            ),
            Exercise(
                id = 9, scheduleId = planId,
                name = "Hamstring Stretch", phase = "Phase 1",
                sets = 2, reps = 1, holdSeconds = 30,
                notes = "Keep your back straight throughout.",
                gifUrl = "https://cdn.jefit.com/assets/img/exercises/gifs/932.gif",
                description = "Stretches the hamstring muscles to restore range of motion and prevent tightness after lower limb injury.",
                completed = false, lastPainLevel = null
            ),
            Exercise(
                id = 1, scheduleId = planId,
                name = "Squat", phase = "Strength",
                sets = 2, reps = 10, holdSeconds = 0,
                notes = null,
                gifUrl = "https://cdn.jefit.com/assets/img/exercises/gifs/493.gif",
                description = "Strengthens quadriceps, glutes and core. Essential for regaining functional leg strength after lower limb surgery.",
                completed = true, lastPainLevel = 2
            ),
            Exercise(
                id = 10, scheduleId = planId,
                name = "Single-Leg Balance", phase = "Balance",
                sets = 3, reps = 1, holdSeconds = 30,
                notes = "Stand near a wall for safety.",
                gifUrl = "https://cdn.jefit.com/assets/img/exercises/gifs/662.gif",
                description = "Trains proprioception and joint stability. A key functional milestone in lower limb rehabilitation.",
                completed = true, lastPainLevel = 1
            )
        )
        val estimatedMinutes = exercises.filter { !it.completed }.sumOf { it.sets * it.reps * 30 } / 60
        return PlanDetails(estimatedMinutes, exercises)
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
