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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/** Which panel of the forgot-password modal is showing — mirrors the approved web design's 4 steps. */
enum class ForgotPasswordStep { EMAIL, CODE, CHOICE, NEW_PASSWORD, SUCCESS }

/** Loading/error status for whichever action the current step's button triggers. */
sealed interface ForgotPasswordOpState {
    data object Idle : ForgotPasswordOpState
    data object Loading : ForgotPasswordOpState
    data class Error(val message: String) : ForgotPasswordOpState
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

    private val _forgotStep = MutableStateFlow(ForgotPasswordStep.EMAIL)
    val forgotStep: StateFlow<ForgotPasswordStep> = _forgotStep.asStateFlow()

    private val _forgotOpState = MutableStateFlow<ForgotPasswordOpState>(ForgotPasswordOpState.Idle)
    val forgotOpState: StateFlow<ForgotPasswordOpState> = _forgotOpState.asStateFlow()

    /** The email the code was sent to — kept here so the CODE step can show "sent to x@y.com" and resend without re-asking. */
    private val _forgotEmail = MutableStateFlow("")
    val forgotEmail: StateFlow<String> = _forgotEmail.asStateFlow()

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

    /** Step EMAIL: sends the 6-digit code, then advances to CODE. */
    fun sendResetCode(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !isValidEmail(trimmed)) {
            _forgotOpState.value = ForgotPasswordOpState.Error("Please enter a valid email address")
            return
        }
        viewModelScope.launch {
            _forgotOpState.value = ForgotPasswordOpState.Loading
            authUseCase.requestPasswordReset(trimmed)
                .onSuccess {
                    _forgotEmail.value = trimmed
                    _forgotOpState.value = ForgotPasswordOpState.Idle
                    _forgotStep.value = ForgotPasswordStep.CODE
                }
                .onFailure { err ->
                    _forgotOpState.value = ForgotPasswordOpState.Error(err.message ?: "Couldn't send the code")
                }
        }
    }

    /** "Resend code" link on the CODE step — same call, stays on CODE either way. */
    fun resendResetCode() {
        val email = _forgotEmail.value
        if (email.isBlank()) return
        viewModelScope.launch {
            _forgotOpState.value = ForgotPasswordOpState.Loading
            authUseCase.requestPasswordReset(email)
                .onSuccess { _forgotOpState.value = ForgotPasswordOpState.Idle }
                .onFailure { err ->
                    _forgotOpState.value = ForgotPasswordOpState.Error(err.message ?: "Couldn't resend the code")
                }
        }
    }

    /** Step CODE: checks the typed 6 digits, then advances to CHOICE. */
    fun verifyResetCode(code: String) {
        if (code.length != 6) {
            _forgotOpState.value = ForgotPasswordOpState.Error("Enter all 6 digits")
            return
        }
        viewModelScope.launch {
            _forgotOpState.value = ForgotPasswordOpState.Loading
            authUseCase.verifyPasswordResetCode(_forgotEmail.value, code)
                .onSuccess {
                    _forgotOpState.value = ForgotPasswordOpState.Idle
                    _forgotStep.value = ForgotPasswordStep.CHOICE
                }
                .onFailure { err ->
                    _forgotOpState.value = ForgotPasswordOpState.Error(err.message ?: "That code didn't match — try again.")
                }
        }
    }

    /** Step CHOICE → "Update password": just moves to NEW_PASSWORD, no network call yet. */
    fun chooseUpdatePassword() {
        _forgotOpState.value = ForgotPasswordOpState.Idle
        _forgotStep.value = ForgotPasswordStep.NEW_PASSWORD
    }

    /** Step CHOICE → "Continue to dashboard": signs the user in with the session the verified code already proved, skipping a password change entirely. */
    fun continueWithoutReset(onSignedIn: () -> Unit) {
        viewModelScope.launch {
            _forgotOpState.value = ForgotPasswordOpState.Loading
            authUseCase.loginWithVerifiedRecoverySession()
                .onSuccess {
                    _forgotOpState.value = ForgotPasswordOpState.Idle
                    onSignedIn()
                }
                .onFailure { err ->
                    _forgotOpState.value = ForgotPasswordOpState.Error(err.message ?: "Couldn't sign you in")
                }
        }
    }

    /** Step NEW_PASSWORD: sets the new password using the verified-code session, then advances to SUCCESS. */
    fun confirmNewPassword(newPassword: String, confirmPassword: String) {
        if (newPassword.length < 6) {
            _forgotOpState.value = ForgotPasswordOpState.Error("Password must be at least 6 characters")
            return
        }
        if (newPassword != confirmPassword) {
            _forgotOpState.value = ForgotPasswordOpState.Error("Passwords don't match")
            return
        }
        viewModelScope.launch {
            _forgotOpState.value = ForgotPasswordOpState.Loading
            authUseCase.confirmPasswordReset(newPassword)
                .onSuccess {
                    _forgotOpState.value = ForgotPasswordOpState.Idle
                    _forgotStep.value = ForgotPasswordStep.SUCCESS
                }
                .onFailure { err ->
                    _forgotOpState.value = ForgotPasswordOpState.Error(err.message ?: "Couldn't reset the password")
                }
        }
    }

    /** "← Use a different email" on the CODE step. */
    fun backToEmailStep() {
        _forgotOpState.value = ForgotPasswordOpState.Idle
        _forgotStep.value = ForgotPasswordStep.EMAIL
    }

    /** Call when the forgot-password dialog is closed/torn down, so a fresh open always starts clean. */
    fun resetForgotPasswordState() {
        _forgotOpState.value = ForgotPasswordOpState.Idle
        _forgotStep.value = ForgotPasswordStep.EMAIL
        _forgotEmail.value = ""
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
