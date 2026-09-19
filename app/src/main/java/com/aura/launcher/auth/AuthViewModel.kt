package com.aura.launcher.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.domain.model.User
import com.aura.launcher.domain.usecase.AuthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/**
 * Phase 4.4: the simple Supabase-email-link forgot-password flow.
 * RequestSent covers both steps of that flow up to the emailed link;
 * [confirmPasswordReset] (fired after the app reopens via the deep link)
 * drives the rest.
 */
sealed interface ForgotPasswordUiState {
    data object Idle : ForgotPasswordUiState
    data object Sending : ForgotPasswordUiState
    data object RequestSent : ForgotPasswordUiState
    data object Confirming : ForgotPasswordUiState
    data object ResetSuccess : ForgotPasswordUiState
    data class Error(val message: String) : ForgotPasswordUiState
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

    private val _forgotPasswordState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val forgotPasswordState: StateFlow<ForgotPasswordUiState> = _forgotPasswordState.asStateFlow()

    /** True once MainActivity has captured a Supabase recovery deep link — tells the UI to show the "set new password" step instead of "enter your email". */
    val hasRecoveryLink: StateFlow<Boolean> = PasswordRecoveryLinkHolder.current
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PasswordRecoveryLinkHolder.current.value != null
        )

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        if (!isValidEmail(email.trim())) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address")
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

    fun signUp(email: String, name: String, password: String, confirmPassword: String) {
        if (email.isBlank() || name.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in all fields")
            return
        }
        if (!isValidEmail(email.trim())) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match")
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

    /** Step 1 of the new Supabase forgot-password flow: sends the reset email. */
    fun requestPasswordReset(email: String) {
        if (email.isBlank() || !isValidEmail(email.trim())) {
            _forgotPasswordState.value = ForgotPasswordUiState.Error("Please enter a valid email address")
            return
        }
        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordUiState.Sending
            authUseCase.requestPasswordReset(email.trim())
                .onSuccess { _forgotPasswordState.value = ForgotPasswordUiState.RequestSent }
                .onFailure { err ->
                    _forgotPasswordState.value = ForgotPasswordUiState.Error(err.message ?: "Couldn't send the reset email")
                }
        }
    }

    /** Step 2: called after the app reopens via the emailed deep link and the user types a new password. */
    fun confirmPasswordReset(newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _forgotPasswordState.value = ForgotPasswordUiState.Error("Password must be at least 6 characters")
            return
        }
        if (newPassword != confirmPassword) {
            _forgotPasswordState.value = ForgotPasswordUiState.Error("Passwords do not match")
            return
        }
        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordUiState.Confirming
            authUseCase.confirmPasswordReset(newPassword)
                .onSuccess { _forgotPasswordState.value = ForgotPasswordUiState.ResetSuccess }
                .onFailure { err ->
                    _forgotPasswordState.value = ForgotPasswordUiState.Error(err.message ?: "Couldn't reset the password")
                }
        }
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordUiState.Idle
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
