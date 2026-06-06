package com.example.limbmotionrecoveryapp.screens.auth

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.MainActivity
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegisterActivity : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels()

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilDoctorId: TextInputLayout

    private val colorGreen = Color.parseColor("#1D9E75")
    private val colorAmber = Color.parseColor("#EF9F27")
    private val colorEmpty = Color.parseColor("#E8EFED")
    private val colorBorder = Color.parseColor("#D8E8E3")
    private val colorError = Color.parseColor("#E53935")

    // 防止旋转屏幕重复弹出 Dialog
    private var pendingDoctorVerified: RegisterViewModel.State.DoctorVerified? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        tilDoctorId = findViewById(R.id.tilDoctorId)

        loadLogo()
        setupBackButton()
        setupFilledStateWatchers()
        setupPasswordStrength()
        setupTermsText()
        setupSignInLink()
        setupVerifyDoctorButton()
        setupCreateButton()
        observeState()
    }

    private fun loadLogo() {
        val imgLogoMini = findViewById<ImageView>(R.id.imgLogoMini)
        try {
            val bitmap = BitmapFactory.decodeStream(assets.open("icons/LimbIcon.png"))
            imgLogoMini.setImageBitmap(bitmap)
        } catch (_: Exception) {}
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupFilledStateWatchers() {
        watchFilledState(R.id.etName, tilName)
        watchFilledState(R.id.etEmail, tilEmail)
    }

    private fun watchFilledState(editTextId: Int, til: TextInputLayout) {
        findViewById<TextInputEditText>(editTextId).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val filled = !s.isNullOrBlank()
                til.setBoxStrokeColorStateList(
                    android.content.res.ColorStateList.valueOf(if (filled) colorGreen else colorBorder)
                )
                til.isEndIconVisible = filled
            }
        })
        til.isEndIconVisible = false
    }

    private fun setupPasswordStrength() {
        val bars = listOf(
            findViewById<View>(R.id.strengthBar1),
            findViewById<View>(R.id.strengthBar2),
            findViewById<View>(R.id.strengthBar3),
            findViewById<View>(R.id.strengthBar4)
        )
        val tvLabel = findViewById<TextView>(R.id.tvStrengthLabel)

        findViewById<TextInputEditText>(R.id.etPassword).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val strength = viewModel.getPasswordStrength(s?.toString() ?: "")
                bars.forEachIndexed { index, bar ->
                    bar.setBackgroundColor(when {
                        index >= strength -> colorEmpty
                        strength == 4 -> colorGreen
                        strength == 3 -> if (index < 2) colorGreen else colorAmber
                        else -> colorAmber
                    })
                }
                tvLabel.text = when (strength) {
                    1 -> "Weak"
                    2 -> "Fair"
                    3 -> "Good password"
                    4 -> "Strong password"
                    else -> ""
                }
                tvLabel.setTextColor(if (strength >= 3) colorGreen else colorAmber)
            }
        })
    }

    private fun setupTermsText() {
        val tvTerms = findViewById<TextView>(R.id.tvTerms)
        val text = SpannableString("I agree to the Terms of Service and Privacy Policy")
        fun span(word: String) {
            val start = text.indexOf(word)
            text.setSpan(ForegroundColorSpan(colorGreen), start, start + word.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(StyleSpan(Typeface.BOLD), start, start + word.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        span("Terms of Service")
        span("Privacy Policy")
        tvTerms.text = text
    }

    private fun setupSignInLink() {
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)
        val text = SpannableString("Already have an account? Sign in")
        val start = text.indexOf("Sign in")
        text.setSpan(ForegroundColorSpan(colorGreen), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(StyleSpan(Typeface.BOLD), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvSignIn.text = text
        tvSignIn.setOnClickListener { finish() }
    }

    private fun setupVerifyDoctorButton() {
        val btnVerify = findViewById<MaterialButton>(R.id.btnVerifyDoctor)
        btnVerify.setOnClickListener {
            val doctorId = findViewById<TextInputEditText>(R.id.etDoctorId).text.toString()
            pendingDoctorVerified = null
            viewModel.verifyDoctor(doctorId)
        }
    }

    private fun setupCreateButton() {
        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateAccount)
        btnCreate.setOnClickListener {
            viewModel.register(
                name = findViewById<TextInputEditText>(R.id.etName).text.toString(),
                email = findViewById<TextInputEditText>(R.id.etEmail).text.toString(),
                password = findViewById<TextInputEditText>(R.id.etPassword).text.toString(),
                confirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword).text.toString(),
                termsAccepted = findViewById<MaterialCheckBox>(R.id.cbTerms).isChecked,
                doctorIdStr = findViewById<TextInputEditText>(R.id.etDoctorId).text.toString()
            )
        }
    }

    private fun observeState() {
        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateAccount)
        val btnVerify = findViewById<MaterialButton>(R.id.btnVerifyDoctor)
        val tvDoctorStatus = findViewById<TextView>(R.id.tvDoctorStatus)

        viewModel.state.observe(this) { state ->
            when (state) {
                is RegisterViewModel.State.Loading -> {
                    btnCreate.isEnabled = false
                    btnVerify.isEnabled = false
                }
                is RegisterViewModel.State.DoctorVerified -> {
                    btnVerify.isEnabled = true
                    btnCreate.isEnabled = true
                    btnVerify.text = "Verify Doctor"
                    // 防止重复弹窗（屏幕旋转等场景）
                    if (pendingDoctorVerified == null) {
                        pendingDoctorVerified = state
                        showDoctorConfirmDialog(state.doctorId, state.doctorName, state.doctorRole)
                    }
                }
                is RegisterViewModel.State.DoctorInvalid -> {
                    btnVerify.isEnabled = true
                    btnCreate.isEnabled = true
                    btnVerify.text = "Verify Doctor"
                    tvDoctorStatus.visibility = View.VISIBLE
                    tvDoctorStatus.text = state.message
                    tvDoctorStatus.setTextColor(colorError)
                    Snackbar.make(btnCreate, state.message, Snackbar.LENGTH_LONG).show()
                }
                is RegisterViewModel.State.Success -> {
                    btnCreate.isEnabled = true
                    btnVerify.isEnabled = true
                    getSharedPreferences("auth", MODE_PRIVATE).edit()
                        .putString("token", state.token)
                        .putInt("userId", state.userId)
                        .putString("userName", state.userName)
                        .putString("userEmail", state.userEmail)
                        .apply()
                    startActivity(Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                }
                is RegisterViewModel.State.Error -> {
                    btnCreate.isEnabled = true
                    btnVerify.isEnabled = true
                    Snackbar.make(btnCreate, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    btnCreate.isEnabled = true
                    btnVerify.isEnabled = true
                }
            }
        }
    }

    private fun showDoctorConfirmDialog(doctorId: Int, doctorName: String, doctorRole: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Doctor")
            .setMessage("Bind to the following clinician?\n\nName: $doctorName\nRole: ${doctorRole.replaceFirstChar { it.uppercase() }}\nID: $doctorId")
            .setPositiveButton("Confirm") { _, _ ->
                updateDoctorVerifiedUI(doctorId, doctorName)
            }
            .setNegativeButton("Cancel") { _, _ ->
                viewModel.clearVerifiedDoctor()
                clearDoctorVerifiedUI()
            }
            .setOnDismissListener {
                pendingDoctorVerified = null
            }
            .setCancelable(false)
            .show()
    }

    private fun updateDoctorVerifiedUI(doctorId: Int, doctorName: String) {
        val btnVerify = findViewById<MaterialButton>(R.id.btnVerifyDoctor)
        val tvDoctorStatus = findViewById<TextView>(R.id.tvDoctorStatus)

        btnVerify.text = "Doctor Verified ✓"
        btnVerify.setTextColor(colorGreen)
        btnVerify.strokeColor = android.content.res.ColorStateList.valueOf(colorGreen)

        tvDoctorStatus.visibility = View.VISIBLE
        tvDoctorStatus.text = "Verified: $doctorName (ID: $doctorId)"
        tvDoctorStatus.setTextColor(colorGreen)
    }

    private fun clearDoctorVerifiedUI() {
        val btnVerify = findViewById<MaterialButton>(R.id.btnVerifyDoctor)
        val tvDoctorStatus = findViewById<TextView>(R.id.tvDoctorStatus)

        btnVerify.text = "Verify Doctor"
        btnVerify.setTextColor(colorGreen)
        btnVerify.strokeColor = android.content.res.ColorStateList.valueOf(colorGreen)

        tvDoctorStatus.visibility = View.GONE
    }
}