package com.example.limbmotionrecoveryapp.screens.plans

data class Exercise(
    val id: Int,
    val name: String,
    val phase: String,
    val sets: Int,
    val reps: Int,
    val holdSeconds: Int,
    val completed: Boolean,
    val lastPainLevel: Int?
) {
    val metaText: String get() {
        val parts = mutableListOf("$sets sets · $reps reps")
        if (holdSeconds > 0) parts.add("${holdSeconds}s hold")
        return parts.joinToString(" · ")
    }
}
