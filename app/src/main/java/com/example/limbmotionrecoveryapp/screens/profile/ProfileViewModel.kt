package com.example.limbmotionrecoveryapp.screens.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.m1.api.V2ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val api = V2ApiClient()

    private val _state = MutableLiveData<ProfileData>()
    val state: LiveData<ProfileData> = _state

    sealed class UpdateResult {
        object Success : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }

    private val _updateResult = MutableLiveData<UpdateResult?>()
    val updateResult: LiveData<UpdateResult?> = _updateResult

    fun load(token: String, cachedName: String, cachedEmail: String) {
        _state.value = ProfileData(name = cachedName, email = cachedEmail)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val me = api.getMe(token)
                val name = me["name"] as? String ?: cachedName
                val email = me["email"] as? String ?: cachedEmail
                val conditionLabel = me["conditionLabel"] as? String ?: ""
                val conditionDate = me["conditionDate"] as? String ?: ""
                val romDegrees = (me["currentRomDegrees"] as? Double)?.toInt() ?: 0
                val adherence = (me["adherencePercent"] as? Double)?.toInt() ?: 0
                val streak = (me["streakWeeks"] as? Double)?.toInt() ?: 0
                _state.postValue(
                    ProfileData(name, email, conditionLabel, conditionDate, romDegrees, adherence, streak)
                )
            } catch (_: Exception) {
                // Keep cached data — extra fields just stay blank
            }
        }
    }

    fun update(userId: Int, token: String, name: String, email: String, newPassword: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = api.updateUser(
                    id = userId,
                    name = name.takeIf { it.isNotBlank() },
                    email = email.takeIf { it.isNotBlank() },
                    password = newPassword?.takeIf { it.isNotBlank() },
                    token = token
                )
                val updatedName = updated["name"] as? String ?: name
                val updatedEmail = updated["email"] as? String ?: email
                _state.postValue(_state.value?.copy(name = updatedName, email = updatedEmail))
                _updateResult.postValue(UpdateResult.Success)
            } catch (e: Exception) {
                _updateResult.postValue(UpdateResult.Error(e.message ?: "Update failed"))
            }
        }
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }
}
