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

    private val colorGreen = Color.parseColor("#1D9E75")
    private val colorAmber = Color.parseColor("#EF9F27")
    private val colorEmpty = Color.parseColor("#E8EFED")
    private val colorBorder = Color.parseColor("#D8E8E3")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)

        loadLogo()
        setupBackButton()
        setupFilledStateWatchers()
        setupPasswordStrength()
        setupTermsText()
        setupSignInLink()
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

    private fun setupCreateButton() {
        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateAccount)
        btnCreate.setOnClickListener {
            viewModel.register(
                name = findViewById<TextInputEditText>(R.id.etName).text.toString(),
                email = findViewById<TextInputEditText>(R.id.etEmail).text.toString(),
                password = findViewById<TextInputEditText>(R.id.etPassword).text.toString(),
                confirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword).text.toString(),
                termsAccepted = findViewById<MaterialCheckBox>(R.id.cbTerms).isChecked
            )
        }
    }

    private fun observeState() {
        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateAccount)

        viewModel.state.observe(this) { state ->
            when (state) {
                is RegisterViewModel.State.Loading -> {
                    btnCreate.isEnabled = false
                }
                is RegisterViewModel.State.Success -> {
                    btnCreate.isEnabled = true
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
                    Snackbar.make(btnCreate, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    btnCreate.isEnabled = true
                }
            }
        }
    }
}