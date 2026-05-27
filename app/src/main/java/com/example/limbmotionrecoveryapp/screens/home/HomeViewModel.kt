package com.example.limbmotionrecoveryapp.screens.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.m1.api.V2ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
                val schedule = api.getSchedule(userId, token)
                val activePlan = schedule.firstOrNull { it["status"] as? String == "active" }

                val planName = activePlan?.let {
                    it["name"] as? String ?: it["title"] as? String
                }
                val startDate = activePlan?.let {
                    it["startDate"] as? String ?: it["start_date"] as? String
                }
                val endDate = activePlan?.let {
                    it["endDate"] as? String ?: it["end_date"] as? String
                }
                val completedSessions = activePlan?.let {
                    (it["completedSessions"] as? Double)?.toInt()
                        ?: (it["completed_sessions"] as? Double)?.toInt() ?: 0
                } ?: 0
                val totalSessions = activePlan?.let {
                    (it["totalSessions"] as? Double)?.toInt()
                        ?: (it["total_sessions"] as? Double)?.toInt() ?: 0
                } ?: 0

                val (weekCurrent, weekTotal, weekFraction) = computeWeekProgress(startDate, endDate)

                _state.postValue(
                    HomeState(
                        userName = userName,
                        activePlanName = planName,
                        recoveryWeekCurrent = weekCurrent,
                        recoveryWeekTotal = weekTotal,
                        weekProgressFraction = weekFraction,
                        completedSessions = completedSessions,
                        totalSessions = totalSessions,
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

    private fun computeWeekProgress(startDate: String?, endDate: String?): Triple<Int, Int, Float> {
        if (startDate == null || endDate == null) return Triple(0, 0, 0f)
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = fmt.parse(startDate) ?: return Triple(0, 0, 0f)
            val end = fmt.parse(endDate) ?: return Triple(0, 0, 0f)
            val now = Date()
            val totalDays = TimeUnit.MILLISECONDS.toDays(end.time - start.time).toInt()
            val elapsedDays = TimeUnit.MILLISECONDS.toDays(now.time - start.time).toInt()
                .coerceIn(0, totalDays)
            val totalWeeks = (totalDays / 7).coerceAtLeast(1)
            val currentWeek = (elapsedDays / 7 + 1).coerceIn(1, totalWeeks)
            val fraction = (elapsedDays.toFloat() / totalDays).coerceIn(0f, 1f)
            Triple(currentWeek, totalWeeks, fraction)
        } catch (_: Exception) {
            Triple(0, 0, 0f)
        }
    }
}
