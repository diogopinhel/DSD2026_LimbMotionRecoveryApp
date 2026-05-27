package com.example.limbmotionrecoveryapp.screens.plans

data class Plan(
    val id: Int,
    val name: String,
    val status: String,
    val startDate: String,
    val endDate: String,
    val totalSessions: Int,
    val completedSessions: Int,
    val phases: List<String>,
    val doctorName: String,
    val todayExercises: Int = 0
) {
    val progressPercent: Int
        get() = if (totalSessions > 0) (completedSessions * 100 / totalSessions) else 0

    val isActive: Boolean get() = status == "active"
    val isUpcoming: Boolean get() = status == "upcoming"
    val isCompleted: Boolean get() = status == "completed"
}
