package com.example.limbmotionrecoveryapp.screens.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.m1.api.V2ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val api = V2ApiClient()

    sealed class State {
        object Idle : State()
        object Loading : State()
        data class Success(val token: String, val userId: Int, val userName: String) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Idle)
    val state: LiveData<State> = _state

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = State.Error("Please fill in all fields")
            return
        }
        _state.value = State.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = api.login(email.trim(), password)
                val token = result["token"] as? String ?: throw RuntimeException("Invalid response")
                val user = result["user"] as? Map<*, *>
                val userId = (user?.get("id") as? Double)?.toInt() ?: 0
                val userName = user?.get("name") as? String ?: ""
                _state.postValue(State.Success(token, userId, userName))
            } catch (e: Exception) {
                _state.postValue(State.Error(e.message ?: "Login failed"))
            }
        }
    }
}
