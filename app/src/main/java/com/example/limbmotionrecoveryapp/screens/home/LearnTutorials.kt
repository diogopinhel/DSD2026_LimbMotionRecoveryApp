package com.example.limbmotionrecoveryapp.screens.home

object LearnTutorials {

    data class Tutorial(
        val title: String,
        val description: String,
        val duration: String,
        val youtubeId: String,
        val thumbColor: String,
        val category: String
    )

    val all = listOf(
        Tutorial(
            title = "Understanding knee recovery",
            description = "Learn the fundamentals of post-surgery knee recovery. Understand what to expect during each phase, how healing progresses, and the key milestones that mark your journey back to full mobility.",
            duration = "5 min",
            youtubeId = "",
            thumbColor = "#D4EDE4",
            category = "Recovery basics"
        ),
        Tutorial(
            title = "Managing pain after surgery",
            description = "Practical techniques to manage discomfort during recovery. Covers safe use of ice/heat therapy, positioning strategies, and how to differentiate normal soreness from warning signs.",
            duration = "6 min",
            youtubeId = "",
            thumbColor = "#E8F0FD",
            category = "Pain management"
        ),
        Tutorial(
            title = "Importance of consistency",
            description = "Discover why daily, consistent effort matters more than intensity. Small regular sessions lead to faster and more durable recovery outcomes than irregular bursts of activity.",
            duration = "4 min",
            youtubeId = "",
            thumbColor = "#FFF3E0",
            category = "Mindset"
        ),
        Tutorial(
            title = "Safe stretching techniques",
            description = "Step-by-step guide to stretches designed for post-surgical rehab. Each movement is demonstrated with proper form, common mistakes to avoid, and modifications for different recovery stages.",
            duration = "8 min",
            youtubeId = "",
            thumbColor = "#F0E8FD",
            category = "Movement"
        ),
        Tutorial(
            title = "Nutrition for faster healing",
            description = "How your diet directly impacts recovery speed. Key nutrients, foods to prioritize, and simple meal tips to support tissue repair and reduce inflammation during rehabilitation.",
            duration = "7 min",
            youtubeId = "",
            thumbColor = "#FDE8EE",
            category = "Nutrition"
        ),
        Tutorial(
            title = "Sleep & recovery science",
            description = "Sleep is when most healing happens. Learn how to optimize your sleep environment and habits to maximize the body's natural repair processes during rehabilitation.",
            duration = "5 min",
            youtubeId = "",
            thumbColor = "#E8F5FD",
            category = "Rest & recovery"
        )
    )
}
