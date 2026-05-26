package com.example.limbmotionrecoveryapp.screens.progress

data class ProgressData(
    val weekLabel: String = "",
    val rom: RomData? = null,
    val adherence: AdherenceData? = null,
    val pain: PainData? = null,
    val weeklySummary: WeeklySummaryData? = null
)

data class RomData(
    val currentDegrees: Int,
    val targetDegrees: Int,
    val weeklyGainDegrees: Int,
    val history: List<RomPoint>
)

data class RomPoint(val date: String, val degrees: Int)

data class AdherenceData(
    val weeklyPercent: Int,
    val completedExercises: Int,
    val totalExercises: Int,
    val skippedExercises: Int,
    val streakWeeks: Int,
    val weekDays: List<DayStatus>
)

data class DayStatus(val day: String, val done: Boolean, val isToday: Boolean = false)

data class PainData(
    val averageThisWeek: Int,
    val changeFromLastWeek: Int,
    val daily: List<PainPoint>
)

data class PainPoint(val date: String, val level: Int)

data class WeeklySummaryData(
    val avgSessionMinutes: Int,
    val activeDays: Int,
    val romGainDegrees: Int
)
