package com.example.limbmotionrecoveryapp.screens.progress

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dsd.m1.api.V2ApiClient
import com.example.limbmotionrecoveryapp.screens.home.PainCheckInActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ProgressViewModel(app: Application) : AndroidViewModel(app) {

    private val api = V2ApiClient()

    data class ProgressState(
        val loading: Boolean = true,
        val weekLabel: String = "",
        val sessionsThisWeek: Int = 0,
        val sessionsVsLastWeek: Int = 0,
        val sessionsByDay: List<Int> = List(7) { 0 },
        val todayDayIndex: Int = 0,
        val avgPainThisWeek: Float = 0f,
        val avgPainLastWeek: Float = 0f,
        val painTrend: List<PainLineChartView.Point> = emptyList(),
        val streakDays: Int = 0
    )

    private val _state = MutableLiveData(ProgressState(loading = false))
    val state: LiveData<ProgressState> = _state

    fun load(userId: Int, token: String) {
        _state.postValue(ProgressState(loading = true))

        viewModelScope.launch {
            val painEntries = loadPainEntries()
            val sessions: List<Map<String, Any?>> = try {
                withContext(Dispatchers.IO) { api.getSessions(userId, token) }
            } catch (_: Exception) { emptyList() }

            _state.postValue(computeState(sessions, painEntries))
        }
    }

    private fun loadPainEntries(): List<PainCheckInActivity.PainEntry> {
        val json = getApplication<Application>()
            .getSharedPreferences("pain_prefs", Context.MODE_PRIVATE)
            .getString("entries", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PainCheckInActivity.PainEntry>>() {}.type
            Gson().fromJson(json, type)
        } catch (_: Exception) { emptyList() }
    }

    private fun computeState(
        sessions: List<Map<String, Any?>>,
        painEntries: List<PainCheckInActivity.PainEntry>
    ): ProgressState {
        val now = Calendar.getInstance()

        val weekStart = (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val weekEnd = (weekStart.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }
        val lastWeekStart = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -7) }

        val labelFmt = SimpleDateFormat("MMM d", Locale.getDefault())
        val weekLabel = "Week of ${labelFmt.format(weekStart.time)} – ${labelFmt.format(weekEnd.time)}"

        fun sessionTs(s: Map<String, Any?>): Long? {
            val raw = (s["created_at"] ?: s["start_time"] ?: s["date"])?.toString() ?: return null
            val fmts = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
                SimpleDateFormat("yyyy-MM-dd", Locale.US)
            )
            for (fmt in fmts) { try { return fmt.parse(raw)?.time } catch (_: Exception) {} }
            return null
        }

        val thisWeekSessions = sessions.filter { s ->
            val ts = sessionTs(s) ?: return@filter false
            ts in weekStart.timeInMillis..weekEnd.timeInMillis
        }
        val lastWeekSessions = sessions.filter { s ->
            val ts = sessionTs(s) ?: return@filter false
            ts in lastWeekStart.timeInMillis until weekStart.timeInMillis
        }

        val byDay = IntArray(7)
        thisWeekSessions.forEach { s ->
            val ts = sessionTs(s) ?: return@forEach
            val dow = (Calendar.getInstance().apply { timeInMillis = ts }
                .get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            byDay[dow]++
        }
        val todayDow = (now.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

        fun painAvg(from: Long, to: Long): Float {
            val sub = painEntries.filter { it.timestamp in from..to }
            return if (sub.isEmpty()) 0f else sub.map { it.level }.average().toFloat()
        }
        val avgThis = painAvg(weekStart.timeInMillis, weekEnd.timeInMillis)
        val avgLast = painAvg(lastWeekStart.timeInMillis, weekStart.timeInMillis)

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val painTrend = painEntries.take(7).reversed().map { entry ->
            val dow = (Calendar.getInstance().apply { timeInMillis = entry.timestamp }
                .get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            PainLineChartView.Point(dayNames[dow], entry.level)
        }

        // Streak: consecutive days going back from today with any session OR pain entry
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val daysWithActivity = (sessions.mapNotNull { s -> sessionTs(s)?.let { sdf.format(Date(it)) } }
            + painEntries.map { sdf.format(Date(it.timestamp)) }).toSet()

        var streak = 0
        val cal = Calendar.getInstance()
        while (sdf.format(cal.time) in daysWithActivity) {
            streak++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }

        return ProgressState(
            loading = false,
            weekLabel = weekLabel,
            sessionsThisWeek = thisWeekSessions.size,
            sessionsVsLastWeek = thisWeekSessions.size - lastWeekSessions.size,
            sessionsByDay = byDay.toList(),
            todayDayIndex = todayDow,
            avgPainThisWeek = avgThis,
            avgPainLastWeek = avgLast,
            painTrend = painTrend,
            streakDays = streak
        )
    }
}
