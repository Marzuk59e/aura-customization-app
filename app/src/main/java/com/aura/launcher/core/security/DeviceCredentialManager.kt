package com.aura.launcher.core.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import com.aura.launcher.domain.model.DeviceSecurityCapability
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.Base64
import java.util.UUID

/**
 * Owns all Android Keystore operations for device-bound password-reset
 * authentication.
 *
 * Security model (see domain.repository.DeviceAuthRepository for the full
 * flow): each user gets one EC keypair per device, generated inside the
 * Android Keystore with `setUserAuthenticationRequired(true)`. The PRIVATE
 * key never leaves the Keystore and this class never reads its raw bytes —
 * only the Keystore itself can use it, and only after the OS has confirmed
 * biometric/device-credential authentication. This class only ever hands out
 * the PUBLIC key (safe to send to the backend) and signatures produced by an
 * already-authenticated `Signature` object.
 *
 * The app never touches actual fingerprint/face/PIN data — that is entirely
 * handled by the platform's BiometricPrompt (see BiometricAuthHelper).
 */
class DeviceCredentialManager(private val context: Context) {

    private val androidKeyStore = "AndroidKeyStore"

    private fun keyAlias(userId: String) = "aura_device_key_${userId}"

    /**
     * Hardware/enrollment capability check — no network, no prompt shown.
     *
     * Below API 30, combining DEVICE_CREDENTIAL with a crypto-bound
     * BiometricPrompt (which is what actually signs the reset challenge)
     * isn't reliably supported by the platform, so on those OS versions this
     * only reports BIOMETRIC_AVAILABLE when a true biometric sensor is
     * enrolled. A PIN/pattern/password-only older device is treated as
     * DEVICE_CREDENTIAL_ONLY and steered to the Firebase email-link fallback
     * instead of the crypto-gated device flow — see ForgotPasswordDialog.
     */
    fun getDeviceSecurityCapability(): DeviceSecurityCapability {
        val biometricManager = BiometricManager.from(context)
        val combinedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val checkAuthenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            combinedAuthenticators
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        return when (biometricManager.canAuthenticate(checkAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> DeviceSecurityCapability.BIOMETRIC_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                val hasScreenLock = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ) == BiometricManager.BIOMETRIC_SUCCESS
                if (hasScreenLock) DeviceSecurityCapability.DEVICE_CREDENTIAL_ONLY
                else DeviceSecurityCapability.NONE_ENROLLED
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                val credentialOnly = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                if (credentialOnly == BiometricManager.BIOMETRIC_SUCCESS) {
                    DeviceSecurityCapability.DEVICE_CREDENTIAL_ONLY
                } else {
                    DeviceSecurityCapability.NOT_SUPPORTED
                }
            }
            else -> DeviceSecurityCapability.NOT_SUPPORTED
        }
    }

    /** True if a Keystore entry already exists for this user on this device. */
    fun hasKey(userId: String): Boolean {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        return keyStore.containsAlias(keyAlias(userId))
    }

    /**
     * Generates a fresh EC (P-256) keypair inside the Android Keystore, gated
     * by biometric or device-credential authentication on every signing
     * operation, and invalidated automatically if the user changes their
     * enrolled biometrics or screen lock. Returns the public key, Base64
     * (X.509 SubjectPublicKeyInfo) encoded, ready to send to the backend.
     *
     * Key generation itself does not require the user to authenticate —
     * only *using* the key (signing a challenge) does. Callers should still
     * gate this behind a BiometricPrompt confirmation at the UI layer so the
     * user consciously opts in to device recovery.
     */
    fun generateDeviceKeyPair(userId: String): String {
        val alias = keyAlias(userId)
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC, androidKeyStore
        )

        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            specBuilder.setUserAuthenticationParameters(
                0, // 0 = required on every single use, no grace period
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            specBuilder.setUserAuthenticationValidityDurationSeconds(-1) // pre-API 30: auth required every use
        }

        keyPairGenerator.initialize(specBuilder.build())
        val keyPair = keyPairGenerator.generateKeyPair()
        return exportPublicKeyBase64(keyPair.public)
    }

    private fun exportPublicKeyBase64(publicKey: PublicKey): String =
        Base64.getEncoder().encodeToString(publicKey.encoded)

    fun exportPublicKeyBase64(userId: String): String? {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        val cert = keyStore.getCertificate(keyAlias(userId)) ?: return null
        return exportPublicKeyBase64(cert.publicKey)
    }

    /**
     * Returns a [Signature] initialized for signing with this user's private
     * key, ready to be wrapped in a `BiometricPrompt.CryptoObject`. Actually
     * calling `.sign()` on it will only succeed AFTER the OS confirms
     * biometric/device-credential authentication via BiometricPrompt.
     *
     * @throws KeyPermanentlyInvalidatedException if the user changed their
     * enrolled biometrics/screen lock since this key was created — the
     * caller must treat this as "device trust lost, needs re-registration".
     * @throws IllegalStateException if no key exists for this user yet.
     */
    fun getSignatureForSigning(userId: String): Signature {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        val alias = keyAlias(userId)
        val privateKey = keyStore.getKey(alias, null) as? PrivateKey
            ?: throw IllegalStateException("No device key registered for this user")

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey) // may throw KeyPermanentlyInvalidatedException
        return signature
    }

    /** Signs [challenge] using an already-authenticated [signature] and returns it Base64-encoded. */
    fun signChallenge(signature: Signature, challenge: String): String {
        signature.update(challenge.toByteArray(Charsets.UTF_8))
        val signedBytes = signature.sign()
        return Base64.getEncoder().encodeToString(signedBytes)
    }

    /** Deletes this user's device key + invalidates local trust (logout, invalidation, "forget this device"). */
    fun deleteKey(userId: String) {
        val keyStore = KeyStore.getInstance(androidKeyStore).apply { load(null) }
        val alias = keyAlias(userId)
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    /** A stable-per-install device identifier sent to the backend alongside the public key. */
    fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences("aura_device_identity", Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
    }
}
