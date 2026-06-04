package com.example.limbmotionrecoveryapp.screens.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProgressViewModel : ViewModel() {

    sealed class State {
        object Loading : State()
        data class Success(val data: ProgressData) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    fun load(userId: Int, token: String) {
        _state.value = State.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // TODO (Sergio): Replace with real V2ApiClient.getProgress(userId, token)
                // when GET /progress/{userId} endpoint is available.
                // For now, use mock data so UI renders correctly.
                delay(800) // simulate network
                val mockData = buildMockProgressData()
                _state.postValue(State.Success(mockData))
            } catch (e: Exception) {
                _state.postValue(State.Error(e.message ?: "Failed to load progress"))
            }
        }
    }

    private fun buildMockProgressData(): ProgressData {
        val today = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        fun daysAgo(n: Int): String {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -n)
            return today
        }

        val romHistory = listOf(
            RomPoint(daysAgo(6), 85),
            RomPoint(daysAgo(5), 87),
            RomPoint(daysAgo(4), 90),
            RomPoint(daysAgo(3), 91),
            RomPoint(daysAgo(2), 93),
            RomPoint(daysAgo(1), 94),
            RomPoint(daysAgo(0), 96)
        )

        val painDaily = listOf(
            PainPoint(daysAgo(6), 6),
            PainPoint(daysAgo(5), 5),
            PainPoint(daysAgo(4), 5),
            PainPoint(daysAgo(3), 4),
            PainPoint(daysAgo(2), 4),
            PainPoint(daysAgo(1), 3),
            PainPoint(daysAgo(0), 3)
        )

        return ProgressData(
            weekLabel = "Week 3",
            rom = RomData(
                currentDegrees = 96,
                targetDegrees = 120,
                weeklyGainDegrees = 5,
                history = romHistory
            ),
            adherence = AdherenceData(
                weeklyPercent = 78,
                completedExercises = 11,
                totalExercises = 14,
                skippedExercises = 3,
                streakWeeks = 4,
                weekDays = listOf(
                    DayStatus("M", true, false),
                    DayStatus("T", true, false),
                    DayStatus("W", true, false),
                    DayStatus("T", false, false),
                    DayStatus("F", true, false),
                    DayStatus("S", false, false),
                    DayStatus("S", true, true)
                )
            ),
            pain = PainData(
                averageThisWeek = 4,
                changeFromLastWeek = -1,
                daily = painDaily
            ),
            weeklySummary = WeeklySummaryData(
                avgSessionMinutes = 18,
                activeDays = 5,
                romGainDegrees = 5
            )
        )
    }
}

