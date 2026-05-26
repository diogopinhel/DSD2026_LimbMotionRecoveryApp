package com.example.limbmotionrecoveryapp.screens.profile

data class ProfileData(
    val name: String,
    val email: String,
    val conditionLabel: String = "",
    val conditionDate: String = "",
    val currentRomDegrees: Int = 0,
    val adherencePercent: Int = 0,
    val streakWeeks: Int = 0
)
