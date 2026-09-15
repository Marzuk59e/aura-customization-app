package com.aura.launcher.core.security

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.Signature
import kotlin.coroutines.resume

/**
 * Outcome of a single BiometricPrompt / device-credential authentication
 * attempt. Every state the platform can hand back is represented explicitly
 * so the ViewModel can show a specific, non-alarming message rather than a
 * generic failure — see section 8/13 of the device-auth spec.
 */
sealed interface DeviceAuthOutcome {
    /** [signature] is the same object passed in, now usable for exactly one sign() call. */
    data class Success(val signature: Signature?) : DeviceAuthOutcome
    data object Cancelled : DeviceAuthOutcome
    data object NoSecurityConfigured : DeviceAuthOutcome
    data object KeyInvalidated : DeviceAuthOutcome
    data class Failed(val message: String) : DeviceAuthOutcome
}

/**
 * Thin coroutine wrapper around androidx.biometric.BiometricPrompt. Always
 * uses the platform's native prompt UI — this app never draws its own
 * fingerprint/face/PIN entry screen (see section 7 of the device-auth spec).
 */
object BiometricAuthHelper {

    /**
     * DEVICE_CREDENTIAL combined with a CryptoObject is only reliably
     * supported from API 30 onward; below that we restrict the crypto-bound
     * prompt to true biometrics (matches DeviceCredentialManager's key
     * auth params and capability check, so the two stay in sync).
     */
    private fun cryptoAuthenticators(): Int {
        val strong = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
        val deviceCredential = androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            strong or deviceCredential
        } else {
            strong
        }
    }

    /**
     * Authenticates using a crypto object bound to a Keystore key. Use this
     * for the real password-reset device verification, where the resulting
     * signature is what gets sent to the backend as proof.
     */
    suspend fun authenticateWithCrypto(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        signature: Signature
    ): DeviceAuthOutcome = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)
        val cryptoObject = BiometricPrompt.CryptoObject(signature)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) {
                    continuation.resume(DeviceAuthOutcome.Success(result.cryptoObject?.signature))
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!continuation.isActive) return
                val outcome = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> DeviceAuthOutcome.Cancelled
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> DeviceAuthOutcome.NoSecurityConfigured
                    else -> DeviceAuthOutcome.Failed(errString.toString())
                }
                continuation.resume(outcome)
            }

            override fun onAuthenticationFailed() {
                // A single failed attempt (e.g. finger not recognized) — the
                // prompt stays open and lets the user retry, so we do NOT
                // resume the continuation here.
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(cryptoAuthenticators())
            .build()

        continuation.invokeOnCancellation { /* nothing to clean up — prompt dismisses itself */ }

        try {
            prompt.authenticate(promptInfo, cryptoObject)
        } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
            if (continuation.isActive) continuation.resume(DeviceAuthOutcome.KeyInvalidated)
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(DeviceAuthOutcome.Failed(e.message ?: "Device verification failed"))
        }
    }

    /**
     * A plain (no crypto object) confirmation prompt, used only as a UX gate
     * before *creating* a new device key during opt-in registration — see
     * DeviceCredentialManager.generateDeviceKeyPair.
     */
    suspend fun authenticateSimple(
        activity: FragmentActivity,
        title: String,
        subtitle: String
    ): DeviceAuthOutcome = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(DeviceAuthOutcome.Success(null))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!continuation.isActive) return
                val outcome = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> DeviceAuthOutcome.Cancelled
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> DeviceAuthOutcome.NoSecurityConfigured
                    else -> DeviceAuthOutcome.Failed(errString.toString())
                }
                continuation.resume(outcome)
            }

            override fun onAuthenticationFailed() { /* let the user retry within the same prompt */ }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
