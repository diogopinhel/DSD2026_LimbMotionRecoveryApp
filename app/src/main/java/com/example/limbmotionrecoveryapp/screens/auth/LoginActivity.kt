package com.example.limbmotionrecoveryapp.screens.auth

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.MainActivity
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        if (prefs.getString("token", null) != null) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        loadLogo()
        setupSignUpLink()
        setupButtons()
        observeState()
    }

    private fun loadLogo() {
        val imgLogo = findViewById<ImageView>(R.id.imgLogo)
        try {
            val bitmap = BitmapFactory.decodeStream(assets.open("icons/LimbIcon.png"))
            imgLogo.setImageBitmap(bitmap)
        } catch (_: Exception) {}
    }

    private fun setupSignUpLink() {
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val text = SpannableString("Don't have an account? Sign up")
        val start = text.indexOf("Sign up")
        text.setSpan(ForegroundColorSpan(Color.parseColor("#1D9E75")), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(StyleSpan(Typeface.BOLD), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvSignUp.text = text
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupButtons() {
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        btnSignIn.setOnClickListener {
            viewModel.login(
                etEmail.text.toString(),
                etPassword.text.toString()
            )
        }
    }

    private fun observeState() {
        val btnSignIn = findViewById<MaterialButton>(R.id.btnSignIn)

        viewModel.state.observe(this) { state ->
            when (state) {
                is LoginViewModel.State.Loading -> {
                    btnSignIn.isEnabled = false
                    btnSignIn.text = "Signing in..."
                }
                is LoginViewModel.State.Success -> {
                    getSharedPreferences("auth", MODE_PRIVATE).edit()
                        .putString("token", state.token)
                        .putInt("userId", state.userId)
                        .putString("userName", state.userName)
                        .putString("userEmail", state.userEmail)
                        .apply()
                    goToMain()
                }
                is LoginViewModel.State.Error -> {
                    btnSignIn.isEnabled = true
                    btnSignIn.text = "Sign In"
                    Snackbar.make(btnSignIn, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    btnSignIn.isEnabled = true
                    btnSignIn.text = "Sign In"
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
