package com.example.limbmotionrecoveryapp.screens.home

object LearnTutorials {

    data class Tutorial(
        val title: String,
        val description: String,
        val duration: String,
        val youtubeId: String,
        val thumbColor: String,
        val category: String,
        val author: String = "",
        val videoDuration: String = ""
    )

    val all = listOf(
        Tutorial(
            title = "Managing pain after surgery",
            description = "Practical techniques to manage discomfort during recovery. Covers safe use of ice/heat therapy, positioning strategies, and how to differentiate normal soreness from warning signs.",
            duration = "5 min",
            youtubeId = "XJicOkMRdBk",
            thumbColor = "#E8F0FD",
            category = "Pain management",
            author = "Hospital for Special Surgery",
            videoDuration = "5:08"
        ),
        Tutorial(
            title = "Safe stretching techniques",
            description = "Step-by-step guide to stretches designed for post-surgical rehab. Each movement is demonstrated with proper form, common mistakes to avoid, and modifications for different recovery stages.",
            duration = "14 min",
            youtubeId = "hw_yl3qmO88",
            thumbColor = "#F0E8FD",
            category = "Movement",
            author = "PT Time with Tim",
            videoDuration = "14:00"
        ),
        Tutorial(
            title = "Nutrition for faster healing",
            description = "How your diet directly impacts recovery speed. Key nutrients, foods to prioritize, and simple meal tips to support tissue repair and reduce inflammation during rehabilitation.",
            duration = "15 min",
            youtubeId = "P9TxjkoCnc8",
            thumbColor = "#FDE8EE",
            category = "Nutrition",
            author = "YOGABODY",
            videoDuration = "15:29"
        ),
        Tutorial(
            title = "How do I know if my knee injury is serious?",
            description = "Not every knee injury needs surgery — but some do. Dr. David Geier, an orthopedic surgeon and sports medicine specialist, walks you through the key warning signs that separate a minor knee issue from one that needs professional attention. Useful both before and during your rehabilitation journey.",
            duration = "3 min",
            youtubeId = "KgvkJa3epdQ",
            thumbColor = "#E6EEF8",
            category = "Injury awareness",
            author = "Dr. David Geier",
            videoDuration = "3:24"
        )
    )
}
