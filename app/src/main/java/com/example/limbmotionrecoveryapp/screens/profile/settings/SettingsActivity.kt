package com.example.limbmotionrecoveryapp.screens.profile.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dsd.m1.api.V2ApiClient
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.screens.auth.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private val apiClient = V2ApiClient()
    private var currentDoctorId: Int = 0
    private var userId: Int = 0
    private var token: String = ""

    // Views
    private lateinit var tvCurrentDoctorName: TextView
    private lateinit var tvCurrentDoctorRole: TextView
    private lateinit var tvCurrentDoctorId: TextView
    private lateinit var tilNewDoctorId: TextInputLayout
    private lateinit var etNewDoctorId: TextInputEditText
    private lateinit var btnVerifyDoctor: MaterialButton
    private lateinit var tvVerifyStatus: TextView
    private lateinit var btnChangeDoctor: MaterialButton

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

        // Unit switch
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
            getSharedPreferences("auth", Context.MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // ====== Doctor Rebinding (UC-M1-06-01) ======
        initDoctorViews()
        loadCredentials()
        loadCurrentDoctor()

        btnVerifyDoctor.setOnClickListener { onVerifyClicked() }
        btnChangeDoctor.setOnClickListener { onChangeDoctorClicked() }
    }

    private fun initDoctorViews() {
        tvCurrentDoctorName = findViewById(R.id.tvCurrentDoctorName)
        tvCurrentDoctorRole = findViewById(R.id.tvCurrentDoctorRole)
        tvCurrentDoctorId = findViewById(R.id.tvCurrentDoctorId)
        tilNewDoctorId = findViewById(R.id.tilNewDoctorId)
        etNewDoctorId = findViewById(R.id.etNewDoctorId)
        btnVerifyDoctor = findViewById(R.id.btnVerifyDoctor)
        tvVerifyStatus = findViewById(R.id.tvVerifyStatus)
        btnChangeDoctor = findViewById(R.id.btnChangeDoctor)
    }

    private fun loadCredentials() {
        val authPrefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        token = authPrefs.getString("token", "") ?: ""
        userId = authPrefs.getInt("userId", 0)
    }

    /**
     * Step 1-2: Display current doctor info (or "Not bound")
     */
    private fun loadCurrentDoctor() {
        if (token.isBlank() || userId == 0) {
            showUnboundState()
            return
        }
        lifecycleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) { apiClient.getMe(token) }
                val doctorId = (user["doctor_id"] as? Number)?.toInt() ?: 0
                currentDoctorId = doctorId

                if (doctorId == 0) {
                    showUnboundState()
                } else {
                    val doctor = withContext(Dispatchers.IO) { apiClient.getUserPublic(doctorId) }
                    val name = doctor["name"]?.toString() ?: "Unknown"
                    val role = doctor["role"]?.toString() ?: "N/A"
                    tvCurrentDoctorName.text = name
                    tvCurrentDoctorRole.text = "Role: $role"
                    tvCurrentDoctorId.text = "ID: $doctorId"
                }
            } catch (e: Exception) {
                showUnboundState()
                Toast.makeText(this@SettingsActivity, "Failed to load doctor info", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUnboundState() {
        tvCurrentDoctorName.text = "Not bound"
        tvCurrentDoctorRole.text = "No doctor currently assigned"
        tvCurrentDoctorId.text = ""
    }

    /**
     * Step 3-6: Verify new doctorId
     */
    private fun onVerifyClicked() {
        val input = etNewDoctorId.text.toString().trim()
        tilNewDoctorId.error = null

        if (input.isEmpty()) {
            tilNewDoctorId.error = "Please enter a doctor ID"
            return
        }
        val newId = input.toIntOrNull()
        if (newId == null || newId <= 0) {
            tilNewDoctorId.error = "Invalid doctor ID"
            return
        }
        if (newId == currentDoctorId) {
            tilNewDoctorId.error = "Already bound to this doctor"
            return
        }

        lifecycleScope.launch {
            try {
                val doctor = withContext(Dispatchers.IO) { apiClient.getUserPublic(newId) }
                val role = doctor["role"]?.toString()
                val name = doctor["name"]?.toString() ?: "Unknown"

                if (role != "clinician") {
                    tilNewDoctorId.error = "Invalid doctor ID"
                    tvVerifyStatus.visibility = View.GONE
                    btnChangeDoctor.visibility = View.GONE
                    return@launch
                }

                tvVerifyStatus.text = "Verified: $name ($role, ID: $newId)"
                tvVerifyStatus.setTextColor(getColor(R.color.colorPrimaryGreen))
                tvVerifyStatus.visibility = View.VISIBLE
                btnChangeDoctor.visibility = View.VISIBLE

            } catch (e: Exception) {
                tilNewDoctorId.error = "Invalid doctor ID or network error"
                tvVerifyStatus.visibility = View.GONE
                btnChangeDoctor.visibility = View.GONE
            }
        }
    }

    /**
     * Step 7-9: Show confirmation dialog, then PATCH new doctorId
     */
    private fun onChangeDoctorClicked() {
        val newId = etNewDoctorId.text.toString().trim().toIntOrNull() ?: return

        lifecycleScope.launch {
            try {
                val doctor = withContext(Dispatchers.IO) { apiClient.getUserPublic(newId) }
                val name = doctor["name"]?.toString() ?: "Unknown"
                val role = doctor["role"]?.toString() ?: "clinician"

                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Confirm Doctor Change")
                        .setMessage("Bind to:\n\n$name\n$role\nID: $newId")
                        .setPositiveButton("Confirm") { _, _ -> performUpdate(newId) }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Failed to load doctor info", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performUpdate(newDoctorId: Int) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    apiClient.updateUser(id = userId, doctorId = newDoctorId, token = token)
                }
                currentDoctorId = newDoctorId
                Toast.makeText(this@SettingsActivity, "Doctor updated successfully", Toast.LENGTH_SHORT).show()

                // Refresh display
                loadCurrentDoctor()
                // Reset input
                etNewDoctorId.text?.clear()
                tvVerifyStatus.visibility = View.GONE
                btnChangeDoctor.visibility = View.GONE
                tilNewDoctorId.error = null

            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}