package com.aura.launcher.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.domain.model.User
import com.aura.launcher.domain.usecase.AuthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val authUseCase: AuthUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = authUseCase.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authUseCase.login(email.trim(), password)
            result.onSuccess { user ->
                _uiState.value = AuthUiState.Success(user)
            }.onFailure { err ->
                _uiState.value = AuthUiState.Error(err.message ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, name: String, password: String) {
        if (email.isBlank() || name.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authUseCase.signUp(email.trim(), name.trim(), password)
            result.onSuccess { user ->
                _uiState.value = AuthUiState.Success(user)
            }.onFailure { err ->
                _uiState.value = AuthUiState.Error(err.message ?: "Sign up failed")
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val guest = authUseCase.continueAsGuest()
            _uiState.value = AuthUiState.Success(guest)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authUseCase.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
