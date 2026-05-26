package com.example.limbmotionrecoveryapp.screens.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsd.m1.api.V2ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _state = MutableLiveData<ProfileData>()
    val state: LiveData<ProfileData> = _state

    fun load(token: String, cachedName: String, cachedEmail: String) {
        _state.value = ProfileData(name = cachedName, email = cachedEmail)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val me = V2ApiClient().getMe(token)
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
}
