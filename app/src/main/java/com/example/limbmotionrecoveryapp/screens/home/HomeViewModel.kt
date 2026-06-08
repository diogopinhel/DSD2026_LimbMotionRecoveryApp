package com.example.limbmotionrecoveryapp.screens.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.m1.api.V2ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val api = V2ApiClient()

    data class HomeState(
        val userName: String = "",
        val activePlanName: String? = null,
        val completedSessions: Int = 0,
        val totalSessions: Int = 0,
        val sensorConnected: Boolean = false,
        val sensorBatteryPct: Int = 0,
        val liveRomDeg: Float? = null,
        val liveMovementMs2: Float? = null,
        val loading: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableLiveData(HomeState())
    val state: LiveData<HomeState> = _state

    fun load(userId: Int, token: String, userName: String) {
        _state.value = HomeState(userName = userName, loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Find active plan (first with status == "pending")
                val schedules = api.getSchedule(userId, token)
                val activePlan = schedules.firstOrNull { it["status"] as? String == "pending" }
                val planName = activePlan?.get("exercise") as? String
                val planId = (activePlan?.get("id") as? Double)?.toInt()

                // 2. Fetch exercise counts for the active plan
                var completedEx = 0
                var totalEx = 0
                if (planId != null) {
                    try {
                        val detail = api.getScheduleExercises(planId, token)
                        val list = detail["exercises"] as? List<*> ?: emptyList<Any>()
                        totalEx = list.size
                        completedEx = list.count { item ->
                            (item as? Map<*, *>)?.get("completed") as? Boolean == true
                        }
                    } catch (_: Exception) {
                        // Plan has no exercises yet — keep 0/0
                    }
                }

                _state.postValue(
                    HomeState(
                        userName = userName,
                        activePlanName = planName,
                        completedSessions = completedEx,
                        totalSessions = totalEx,
                        loading = false
                    )
                )
            } catch (e: Exception) {
                _state.postValue(
                    HomeState(userName = userName, error = e.message, loading = false)
                )
            }
        }
    }

    fun setSensorConnected(connected: Boolean, batteryPct: Int = 0) {
        _state.value = _state.value?.copy(sensorConnected = connected, sensorBatteryPct = batteryPct)
    }

    fun updateLiveSensorData(romDeg: Float, movementMs2: Float) {
        _state.value = _state.value?.copy(liveRomDeg = romDeg, liveMovementMs2 = movementMs2)
    }
}
