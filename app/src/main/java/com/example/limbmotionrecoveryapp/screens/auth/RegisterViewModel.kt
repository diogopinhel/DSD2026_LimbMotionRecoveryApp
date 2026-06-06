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
        data class DoctorVerified(val doctorId: Int, val doctorName: String, val doctorRole: String) : State()
        data class DoctorInvalid(val message: String) : State()
        data class Success(val token: String, val userId: Int, val userName: String, val userEmail: String) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Idle)
    val state: LiveData<State> = _state

    // 缓存验证通过的医生信息，用于注册时绑定
    private var verifiedDoctorId: Int? = null

    /** 步骤 1：验证医生 ID（无需登录 Token，调用 V2 公开接口） */
    fun verifyDoctor(doctorIdStr: String) {
        if (doctorIdStr.isBlank()) {
            _state.value = State.DoctorInvalid("Please enter a Doctor ID")
            return
        }
        val doctorId = doctorIdStr.toIntOrNull()
        if (doctorId == null || doctorId <= 0) {
            _state.value = State.DoctorInvalid("Invalid Doctor ID format")
            return
        }

        _state.value = State.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 注册时用户尚未登录，V2 GET /users/:id 无需鉴权
                // 若 V2ApiClient.getUser 强制要求 token，请改用 getUserPublic 或传空字符串
                val result = api.getUser(doctorId, "")
                val role = result["role"] as? String
                val name = result["name"] as? String ?: "Unknown"
                if (role == "clinician") {
                    verifiedDoctorId = doctorId
                    _state.postValue(State.DoctorVerified(doctorId, name, role))
                } else {
                    verifiedDoctorId = null
                    _state.postValue(State.DoctorInvalid("Invalid doctor ID: user is not a clinician (role=${role ?: "unknown"})"))
                }
            } catch (e: Exception) {
                verifiedDoctorId = null
                _state.postValue(State.DoctorInvalid(e.message ?: "Failed to verify doctor. Please check the ID and try again."))
            }
        }
    }

    /** 清除已验证的医生（用户取消确认对话框时调用） */
    fun clearVerifiedDoctor() {
        verifiedDoctorId = null
        _state.value = State.Idle
    }

    /** 步骤 2：注册并绑定医生 */
    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean,
        doctorIdStr: String
    ) {
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

        val doctorId = verifiedDoctorId
        if (doctorId == null) {
            _state.value = State.Error("Please verify your doctor ID before registering")
            return
        }

        val inputDoctorId = doctorIdStr.toIntOrNull()
        if (inputDoctorId != doctorId) {
            verifiedDoctorId = null
            _state.value = State.Error("Doctor ID changed after verification. Please verify again.")
            return
        }

        _state.value = State.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 注册（V2 直接返回 token）
                val registerResult = api.register(name.trim(), email.trim(), password)
                val token = registerResult["token"] as? String
                    ?: throw RuntimeException("Invalid response: missing token")

                val user = registerResult["user"] as? Map<*, *>
                val userId = (user?.get("id") as? Double)?.toInt() ?: 0
                val userName = user?.get("name") as? String ?: name.trim()
                val userEmail = user?.get("email") as? String ?: email.trim()

                // 2. 绑定医生（PATCH /users/:id）
                if (userId > 0) {
                    api.updateUser(id = userId, doctorId = doctorId, token = token)
                }

                _state.postValue(State.Success(token, userId, userName, userEmail))
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