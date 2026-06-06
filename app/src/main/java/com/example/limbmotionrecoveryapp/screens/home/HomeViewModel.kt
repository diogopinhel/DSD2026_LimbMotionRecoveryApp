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
        val recoveryWeekCurrent: Int = 0,
        val recoveryWeekTotal: Int = 0,
        val weekProgressFraction: Float = 0f,
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
                // 1. 获取日程列表，找第一个 pending 的计划作为"当前计划"
                val schedule = api.getSchedule(userId, token)
                val pendingPlan = schedule.firstOrNull { it["status"] as? String == "pending" }
                val planName = pendingPlan?.let { it["exercise"] as? String }

                // 2. 获取进度统计（completedExercises, totalExercises, weeklyPercent）
                val progress = api.getProgress(userId, token)
                val adherence = progress["adherence"] as? Map<*, *>
                val completedExercises = (adherence?.get("completedExercises") as? Double)?.toInt() ?: 0
                val totalExercises = (adherence?.get("totalExercises") as? Double)?.toInt() ?: 0
                val weeklyPercent = (adherence?.get("weeklyPercent") as? Double)?.toInt() ?: 0

                // 3. 解析 weekLabel（如 "Week 3 of 6"）
                val weekLabel = progress["weekLabel"] as? String
                val (weekCurrent, weekTotal) = parseWeekLabel(weekLabel)

                _state.postValue(
                    HomeState(
                        userName = userName,
                        activePlanName = planName,
                        recoveryWeekCurrent = weekCurrent,
                        recoveryWeekTotal = weekTotal,
                        weekProgressFraction = weeklyPercent / 100f,
                        completedSessions = completedExercises,
                        totalSessions = totalExercises,
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

    /** 解析 "Week 3 of 6" → (current, total) */
    private fun parseWeekLabel(weekLabel: String?): Pair<Int, Int> {
        if (weekLabel == null) return Pair(0, 0)
        val regex = """Week (\d+) of (\d+)""".toRegex()
        val match = regex.find(weekLabel)
        return if (match != null) {
            val current = match.groupValues[1].toIntOrNull() ?: 0
            val total = match.groupValues[2].toIntOrNull() ?: 0
            Pair(current, total)
        } else {
            Pair(0, 0)
        }
    }
}