package com.example.limbmotionrecoveryapp.screens.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProgressViewModel : ViewModel() {

    sealed class State {
        object Loading : State()
        // ❌ V2 ENDPOINT MISSING — GET /progress/{userId}
        // See docs/V2_API_REQUIREMENTS.md — section 4 — Priority 🟠
        data class EndpointMissing(val message: String = "Progress endpoint not yet available.\nSee docs/V2_API_REQUIREMENTS.md") : State()
        data class Success(val data: ProgressData) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    fun load(userId: Int, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // ❌ V2 ENDPOINT MISSING: GET /progress/{userId}
            // When V2 provides this endpoint, replace below with the real call:
            //
            //   val api = V2ApiClient()
            //   val response = api.getProgress(userId, token)  // to be added to V2ApiClient
            //   _state.postValue(State.Success(parseProgressData(response)))
            //
            _state.postValue(State.EndpointMissing())
        }
    }
}
