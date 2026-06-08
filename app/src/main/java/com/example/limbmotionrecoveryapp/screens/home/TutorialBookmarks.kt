package com.example.limbmotionrecoveryapp.screens.home

import android.content.Context

object TutorialBookmarks {
    private const val PREFS = "tutorial_bookmarks"
    private const val KEY = "saved_titles"

    fun save(context: Context, title: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY, emptySet())!!.toMutableSet()
        current.add(title)
        prefs.edit().putStringSet(KEY, current).apply()
    }

    fun remove(context: Context, title: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY, emptySet())!!.toMutableSet()
        current.remove(title)
        prefs.edit().putStringSet(KEY, current).apply()
    }

    fun isSaved(context: Context, title: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY, emptySet())!!.contains(title)
    }

    fun getAll(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY, emptySet())!!.toSet()
    }
}
