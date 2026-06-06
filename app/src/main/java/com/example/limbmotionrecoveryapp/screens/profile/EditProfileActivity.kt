package com.example.limbmotionrecoveryapp.screens.profile

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class EditProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilNewPassword = findViewById(R.id.tilNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSave = findViewById(R.id.btnSave)

        val prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val userId = prefs.getInt("userId", -1)

        etName.setText(intent.getStringExtra("name") ?: "")
        etEmail.setText(intent.getStringExtra("email") ?: "")

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        btnSave.setOnClickListener { attemptSave(userId, token) }

        observeResult(prefs)
    }

    private fun attemptSave(userId: Int, token: String) {
        tilName.error = null
        tilEmail.error = null
        tilNewPassword.error = null
        tilConfirmPassword.error = null

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val newPassword = etNewPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (name.isBlank()) {
            tilName.error = "Name cannot be empty"
            return
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"
            return
        }
        if (newPassword.isNotEmpty()) {
            if (newPassword.length < 6) {
                tilNewPassword.error = "Password must be at least 6 characters"
                return
            }
            if (newPassword != confirmPassword) {
                tilConfirmPassword.error = "Passwords do not match"
                return
            }
        }

        hideKeyboard()
        btnSave.isEnabled = false
        btnSave.text = "Saving…"

        viewModel.update(
            userId = userId,
            token = token,
            name = name,
            email = email,
            newPassword = newPassword.takeIf { it.isNotEmpty() }
        )
    }

    private fun observeResult(prefs: android.content.SharedPreferences) {
        viewModel.updateResult.observe(this) { result ->
            result ?: return@observe
            viewModel.clearUpdateResult()

            btnSave.isEnabled = true
            btnSave.text = "Save Changes"

            when (result) {
                is ProfileViewModel.UpdateResult.Success -> {
                    val name = etName.text.toString().trim()
                    val email = etEmail.text.toString().trim()
                    prefs.edit()
                        .putString("userName", name)
                        .putString("userEmail", email)
                        .apply()
                    setResult(RESULT_OK)
                    finish()
                }
                is ProfileViewModel.UpdateResult.Error -> {
                    Snackbar.make(btnSave, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun hideKeyboard() {
        currentFocus?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}
