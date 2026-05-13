package com.example.limbmotionrecoveryapp.screens.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.m1.api.V2ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val api = V2ApiClient()

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(val token: String, val userId: Int, val userName: String) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Idle)
    val state: LiveData<State> = _state

    fun register(name: String, email: String, password: String, confirmPassword: String, termsAccepted: Boolean) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _state.value = State.Error("Please fill in all fields")
            return
        }
        if (password != confirmPassword) {
            _state.value = State.Error("Passwords do not match")
            return
        }
        if (password.length < 6) {
            _state.value = State.Error("Password must be at least 6 characters")
            return
        }
        if (!termsAccepted) {
            _state.value = State.Error("Please accept the Terms of Service")
            return
        }
        _state.value = State.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                api.register(name.trim(), email.trim(), password)
                val loginResult = api.login(email.trim(), password)
                val token = loginResult["token"] as? String ?: throw RuntimeException("Invalid response")
                val user = loginResult["user"] as? Map<*, *>
                val userId = (user?.get("id") as? Double)?.toInt() ?: 0
                val userName = user?.get("name") as? String ?: name.trim()
                _state.postValue(State.Success(token, userId, userName))
            } catch (e: Exception) {
                _state.postValue(State.Error(e.message ?: "Registration failed"))
            }
        }
    }

    fun getPasswordStrength(password: String): Int {
        if (password.isEmpty()) return 0
        var score = 0
        if (password.length >= 6) score++
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return score.coerceAtMost(4)
    }
}
