package com.example.limbmotionrecoveryapp.screens.profile.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.screens.auth.LoginActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        // Language spinner
        val spinnerLanguage = findViewById<Spinner>(R.id.spinnerLanguage)
        val languages = listOf("English", "中文")
        spinnerLanguage.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        val savedLang = prefs.getString("pref_language", "English") ?: "English"
        spinnerLanguage.setSelection(languages.indexOf(savedLang).coerceAtLeast(0))

        // Unit switch (degrees vs radians)
        val switchUnit = findViewById<SwitchCompat>(R.id.switchUnit)
        switchUnit.isChecked = prefs.getBoolean("pref_unit_radians", false)
        switchUnit.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_unit_radians", isChecked).apply()
        }

        // Notifications switch
        val switchNotifications = findViewById<SwitchCompat>(R.id.switchNotifications)
        switchNotifications.isChecked = prefs.getBoolean("pref_notifications", true)
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_notifications", isChecked).apply()
        }

        // Dark mode switch
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switchDarkMode)
        switchDarkMode.isChecked = prefs.getBoolean("pref_dark_mode", false)
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pref_dark_mode", isChecked).apply()
        }

        // Logout button
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            // Clear auth prefs
            getSharedPreferences("auth", Context.MODE_PRIVATE).edit()
                .clear()
                .apply()
            // Navigate to LoginActivity and clear back stack
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
