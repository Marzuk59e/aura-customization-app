package com.aura.launcher.auth

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.core.security.BiometricAuthHelper
import com.aura.launcher.core.security.DeviceAuthOutcome
import com.aura.launcher.core.security.DeviceCredentialManager
import com.aura.launcher.domain.model.DeviceSecurityCapability
import com.aura.launcher.domain.model.DeviceTrustCheck
import com.aura.launcher.domain.model.DeviceVerificationResult
import com.aura.launcher.domain.model.DeviceLoginResult
import com.aura.launcher.domain.model.TrustedAccountSummary
import com.aura.launcher.domain.usecase.DeviceAuthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives BOTH:
 *  1. Post-login device registration ("enable device recovery?"), and
 *  2. The password-reset device verification step that replaces the old
 *     simulated OTP flow inside ForgotPasswordDialog.
 *
 * Every state the UI can hit is modeled explicitly (see section 12 of the
 * device-auth spec) — there is deliberately no single "isBiometricEnabled"
 * boolean anywhere in this class; trust is only ever established via a
 * backend-verified signature.
 */
sealed interface DeviceVerifyUiState {
    data object Idle : DeviceVerifyUiState
    data object CheckingLocalAccounts : DeviceVerifyUiState
    data class ChooseAccount(val accounts: List<TrustedAccountSummary>) : DeviceVerifyUiState
    data object CheckingTrust : DeviceVerifyUiState
    data object NotTrustedDevice : DeviceVerifyUiState
    data object NoSecurityConfigured : DeviceVerifyUiState
    data object Authenticating : DeviceVerifyUiState
    data object VerifyingWithBackend : DeviceVerifyUiState
    data class VerifiedReady(val userId: String, val deviceId: String, val challenge: String) : DeviceVerifyUiState
    data object ResetSuccess : DeviceVerifyUiState
    /** Biometric signature verified — forgot-password now signs the user straight in instead of asking for a new password. */
    data class LoggedIn(val signInToken: String) : DeviceVerifyUiState
    data object Cancelled : DeviceVerifyUiState
    data class Failed(val message: String) : DeviceVerifyUiState
    data object KeyInvalidated : DeviceVerifyUiState
}

sealed interface DeviceRegisterUiState {
    data object Idle : DeviceRegisterUiState
    data object NotEligible : DeviceRegisterUiState   // no biometric/device-credential hardware/enrollment
    data object AlreadyRegistered : DeviceRegisterUiState
    data object Prompting : DeviceRegisterUiState
    data object Registering : DeviceRegisterUiState
    data object Registered : DeviceRegisterUiState
    data object Skipped : DeviceRegisterUiState
    data class Failed(val message: String) : DeviceRegisterUiState
}

class DeviceAuthViewModel(
    private val deviceAuthUseCase: DeviceAuthUseCase,
    private val deviceCredentialManager: DeviceCredentialManager
) : ViewModel() {

    private val _registerState = MutableStateFlow<DeviceRegisterUiState>(DeviceRegisterUiState.Idle)
    val registerState: StateFlow<DeviceRegisterUiState> = _registerState.asStateFlow()

    private val _verifyState = MutableStateFlow<DeviceVerifyUiState>(DeviceVerifyUiState.Idle)
    val verifyState: StateFlow<DeviceVerifyUiState> = _verifyState.asStateFlow()

    /** How many biometric attempts have been used this forgot-password session (0..MAX_LOGIN_ATTEMPTS). */
    private val _attemptsUsed = MutableStateFlow(0)
    val attemptsUsed: StateFlow<Int> = _attemptsUsed.asStateFlow()

    companion object {
        const val MAX_LOGIN_ATTEMPTS = 3
    }

    // --- Registration (offered right after a normal login) ---------------

    fun getCapability(): DeviceSecurityCapability = deviceAuthUseCase.getDeviceSecurityCapability()

    suspend fun shouldOfferRegistration(userId: String): Boolean {
        val capability = getCapability()
        if (capability != DeviceSecurityCapability.BIOMETRIC_AVAILABLE) return false
        return !deviceAuthUseCase.hasLocalDeviceKey(userId)
    }

    fun registerDevice(activity: FragmentActivity, userId: String, email: String, deviceLabel: String) {
        viewModelScope.launch {
            _registerState.value = DeviceRegisterUiState.Prompting
            val outcome = BiometricAuthHelper.authenticateSimple(
                activity = activity,
                title = "Enable device recovery",
                subtitle = "Confirm it's you to turn this on"
            )
            when (outcome) {
                is DeviceAuthOutcome.Success -> {
                    _registerState.value = DeviceRegisterUiState.Registering
                    deviceAuthUseCase.registerDevice(userId, email, deviceLabel)
                        .onSuccess { _registerState.value = DeviceRegisterUiState.Registered }
                        .onFailure {
                            _registerState.value = DeviceRegisterUiState.Failed(
                                it.message ?: "Couldn't enable device recovery. Please try again."
                            )
                        }
                }
                DeviceAuthOutcome.Cancelled -> _registerState.value = DeviceRegisterUiState.Skipped
                DeviceAuthOutcome.NoSecurityConfigured -> _registerState.value = DeviceRegisterUiState.NotEligible
                DeviceAuthOutcome.KeyInvalidated -> _registerState.value =
                    DeviceRegisterUiState.Failed("Your device security settings have changed. Please sign in again.")
                is DeviceAuthOutcome.Failed -> _registerState.value =
                    DeviceRegisterUiState.Failed("Device verification was unsuccessful. Please try again.")
            }
        }
    }

    fun skipRegistration() {
        _registerState.value = DeviceRegisterUiState.Skipped
    }

    fun resetRegisterState() {
        _registerState.value = DeviceRegisterUiState.Idle
    }

    // --- Password-reset device verification -------------------------------

    /**
     * Entry point for the redesigned forgot-password flow: never asks for an
     * email up front. Looks at every account this device already has a key
     * for (TrustedAccountSummary) and decides what to show next — zero
     * accounts skips straight to NotTrustedDevice (email fallback), exactly
     * one proceeds automatically, more than one shows ChooseAccount.
     */
    fun startForgotPassword() {
        viewModelScope.launch {
            _attemptsUsed.value = 0
            _verifyState.value = DeviceVerifyUiState.CheckingLocalAccounts
            val accounts = deviceAuthUseCase.getAllTrustedAccounts()
            when {
                accounts.isEmpty() -> _verifyState.value = DeviceVerifyUiState.NotTrustedDevice
                accounts.size == 1 -> checkDeviceTrust(accounts.first().email)
                else -> _verifyState.value = DeviceVerifyUiState.ChooseAccount(accounts)
            }
        }
    }

    /** Called when the user taps one account in ChooseAccount. */
    fun pickAccount(email: String) {
        checkDeviceTrust(email)
    }

    /** Step 1: ask the backend if this device is trusted for [email]. */
    fun checkDeviceTrust(email: String) {
        viewModelScope.launch {
            _verifyState.value = DeviceVerifyUiState.CheckingTrust
            deviceAuthUseCase.checkDeviceTrust(email)
                .onSuccess { result ->
                    _verifyState.value = when (result) {
                        is DeviceTrustCheck.Trusted -> {
                            val localUserId = deviceAuthUseCase.getLocalUserIdForEmail(email)
                            if (localUserId == null) {
                                // Backend still remembers a registration, but this
                                // device no longer holds the matching private key
                                // (e.g. app data was cleared) — can't sign, so this
                                // is functionally "not trusted" from here on.
                                DeviceVerifyUiState.NotTrustedDevice
                            } else {
                                DeviceVerifyUiState.VerifiedReady(localUserId, result.deviceId, result.challenge)
                            }
                        }
                        DeviceTrustCheck.NotTrusted -> DeviceVerifyUiState.NotTrustedDevice
                    }
                }
                .onFailure { _verifyState.value = DeviceVerifyUiState.Failed(it.message ?: "Couldn't reach the server. Please check your connection.") }
        }
    }

    /** Step 2: run BiometricPrompt against the Keystore key, then verify with the backend. */
    fun authenticateAndReset(
        activity: FragmentActivity,
        userId: String,
        email: String,
        deviceId: String,
        challenge: String,
        newPassword: String
    ) {
        viewModelScope.launch {
            _verifyState.value = DeviceVerifyUiState.Authenticating

            val signature = try {
                deviceCredentialManager.getSignatureForSigning(userId)
            } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
                _verifyState.value = DeviceVerifyUiState.KeyInvalidated
                return@launch
            } catch (e: Exception) {
                _verifyState.value = DeviceVerifyUiState.Failed("This device isn't registered for account recovery.")
                return@launch
            }

            val outcome = BiometricAuthHelper.authenticateWithCrypto(
                activity = activity,
                title = "Verify it's you",
                subtitle = "Use your device security to continue",
                signature = signature
            )

            when (outcome) {
                is DeviceAuthOutcome.Success -> {
                    val sig = outcome.signature
                    if (sig == null) {
                        _verifyState.value = DeviceVerifyUiState.Failed("Device verification was unsuccessful. Please try again.")
                        return@launch
                    }
                    val signatureBase64 = deviceCredentialManager.signChallenge(sig, challenge)
                    _verifyState.value = DeviceVerifyUiState.VerifyingWithBackend
                    deviceAuthUseCase.verifyDeviceAndResetPassword(
                        email = email,
                        deviceId = deviceId,
                        challenge = challenge,
                        signatureBase64 = signatureBase64,
                        newPassword = newPassword
                    ).onSuccess { result ->
                        _verifyState.value = when (result) {
                            is DeviceVerificationResult.Success -> DeviceVerifyUiState.ResetSuccess
                            is DeviceVerificationResult.Failed -> DeviceVerifyUiState.Failed(result.message)
                        }
                    }.onFailure {
                        _verifyState.value = DeviceVerifyUiState.Failed(it.message ?: "Couldn't reach the server. Please check your connection.")
                    }
                }
                DeviceAuthOutcome.Cancelled -> _verifyState.value = DeviceVerifyUiState.Cancelled
                DeviceAuthOutcome.NoSecurityConfigured -> _verifyState.value = DeviceVerifyUiState.NoSecurityConfigured
                DeviceAuthOutcome.KeyInvalidated -> _verifyState.value = DeviceVerifyUiState.KeyInvalidated
                is DeviceAuthOutcome.Failed -> _verifyState.value =
                    DeviceVerifyUiState.Failed("Device verification was unsuccessful. Please try again.")
            }
        }
    }

    /**
     * Login-flow sibling of [authenticateAndReset]: no new password is
     * collected — a verified signature signs the user straight in. Counts
     * attempts and, once MAX_LOGIN_ATTEMPTS is used up, drops to
     * NotTrustedDevice so the UI shows the email fallback instead of another
     * "try again" button.
     */
    fun authenticateAndLogin(
        activity: FragmentActivity,
        userId: String,
        email: String,
        deviceId: String,
        challenge: String
    ) {
        viewModelScope.launch {
            _verifyState.value = DeviceVerifyUiState.Authenticating

            val signature = try {
                deviceCredentialManager.getSignatureForSigning(userId)
            } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
                _verifyState.value = DeviceVerifyUiState.KeyInvalidated
                return@launch
            } catch (e: Exception) {
                _verifyState.value = DeviceVerifyUiState.Failed("This device isn't registered for account recovery.")
                return@launch
            }

            val outcome = BiometricAuthHelper.authenticateWithCrypto(
                activity = activity,
                title = "Verify it's you",
                subtitle = "Use your device security to sign in",
                signature = signature
            )

            when (outcome) {
                is DeviceAuthOutcome.Success -> {
                    val sig = outcome.signature
                    if (sig == null) {
                        registerFailedAttempt(email, "Device verification was unsuccessful. Please try again.")
                        return@launch
                    }
                    val signatureBase64 = deviceCredentialManager.signChallenge(sig, challenge)
                    _verifyState.value = DeviceVerifyUiState.VerifyingWithBackend
                    deviceAuthUseCase.verifyDeviceAndLogin(
                        email = email,
                        deviceId = deviceId,
                        challenge = challenge,
                        signatureBase64 = signatureBase64
                    ).onSuccess { result ->
                        when (result) {
                            is DeviceLoginResult.Success -> _verifyState.value = DeviceVerifyUiState.LoggedIn(result.signInToken)
                            is DeviceLoginResult.Failed -> registerFailedAttempt(email, result.message)
                        }
                    }.onFailure {
                        // Network/server error — doesn't count against the 3 tries, nothing to retry differently.
                        _verifyState.value = DeviceVerifyUiState.Failed(it.message ?: "Couldn't reach the server. Please check your connection.")
                    }
                }
                DeviceAuthOutcome.Cancelled -> registerFailedAttempt(email, null, cancelled = true)
                DeviceAuthOutcome.NoSecurityConfigured -> _verifyState.value = DeviceVerifyUiState.NoSecurityConfigured
                DeviceAuthOutcome.KeyInvalidated -> _verifyState.value = DeviceVerifyUiState.KeyInvalidated
                is DeviceAuthOutcome.Failed -> registerFailedAttempt(email, "Device verification was unsuccessful. Please try again.")
            }
        }
    }

    /**
     * Counts one biometric login attempt against MAX_LOGIN_ATTEMPTS. Under
     * the limit, surfaces Cancelled/Failed so the UI can offer "try again"
     * (which re-requests a fresh challenge via checkDeviceTrust); at the
     * limit, drops straight to NotTrustedDevice so the UI shows the email
     * fallback instead.
     */
    private fun registerFailedAttempt(email: String, message: String?, cancelled: Boolean = false) {
        _attemptsUsed.value += 1
        if (_attemptsUsed.value >= MAX_LOGIN_ATTEMPTS) {
            _verifyState.value = DeviceVerifyUiState.NotTrustedDevice
        } else {
            _verifyState.value = if (cancelled) DeviceVerifyUiState.Cancelled
            else DeviceVerifyUiState.Failed(message ?: "Device verification was unsuccessful. Please try again.")
        }
    }

    fun sendFallbackEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            deviceAuthUseCase.sendFallbackResetEmail(email)
                .onSuccess { onResult(true, null) }
                .onFailure { onResult(false, it.message) }
        }
    }

    fun resetVerifyState() {
        _verifyState.value = DeviceVerifyUiState.Idle
    }
}
