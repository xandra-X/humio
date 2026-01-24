package com.example.hrmanagement.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hrmanagement.data.profile.ProfileResponse
import com.example.hrmanagement.network.RetrofitClient
import com.example.hrmanagement.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _state = MutableStateFlow<ProfileResponse?>(null)
    val state: StateFlow<ProfileResponse?> = _state

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error


    fun loadProfile() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = false
            try {
                val token = sessionManager.fetchAuthToken()
                if (token == null) {
                    _error.value = true
                    return@launch
                }

                val response = RetrofitClient.profileApi.getProfile("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    _state.value = response.body()
                } else {
                    _error.value = true
                }
            } catch (e: Exception) {
                _error.value = true
            } finally {
                _loading.value = false
            }
        }
    }
}
