package com.aura.launcher.domain.repository

import com.aura.launcher.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getAllApps(): Flow<List<AppInfo>>
    fun searchApps(query: String): Flow<List<AppInfo>>
    suspend fun refreshInstalledApps()
    suspend fun removeAppByPackage(packageName: String)
    suspend fun launchApp(packageName: String, activityName: String?): Boolean
}

interface HomeRepository {
    fun getItemsForPage(pageIndex: Int): Flow<List<HomeItem>>
    fun getDockItems(): Flow<List<HomeItem>>
    fun getAllHomeItems(): Flow<List<HomeItem>>
    suspend fun addHomeItem(item: HomeItem): Long
    suspend fun updateHomeItem(item: HomeItem)
    suspend fun removeHomeItem(id: Long)
    suspend fun clearAndReplaceAll(items: List<HomeItem>)
    suspend fun initializeDefaultLayout(installedApps: List<AppInfo>)
    suspend fun getFolder(folderId: Long): Folder?
    suspend fun createFolder(title: String, color: String, appKeys: List<String>): Long
    suspend fun updateFolder(folderId: Long, title: String, appKeys: List<String>)
    suspend fun deleteFolder(folderId: Long)
}

interface AuthRepository {
    val currentUserFlow: Flow<User?>
    suspend fun getCurrentUser(): User?
    suspend fun signUp(email: String, name: String, passwordHash: String): Result<User>
    suspend fun login(email: String, passwordHash: String): Result<User>
    /** Completes sign-in using a Firebase custom token (e.g. from device-verified login) instead of a password. */
    suspend fun loginWithCustomToken(signInToken: String): Result<User>
    suspend fun continueAsGuest(): User
    suspend fun logout()
}

/**
 * Handles secure, device-bound authentication used to replace the OTP step
 * of the password-reset flow. The app NEVER decides trust on its own — every
 * method that matters for security round-trips to the backend, which is the
 * only source of truth for whether a device is actually trusted.
 */
interface DeviceAuthRepository {
    /** Checks hardware/enrollment state via BiometricManager. No network call. */
    fun getDeviceSecurityCapability(): DeviceSecurityCapability

    /** True if THIS device already has a local Keystore key for [userId]. Local hint only. */
    suspend fun hasLocalDeviceKey(userId: String): Boolean

    /**
     * Creates a device-bound Keystore keypair (gated by biometric/device
     * credential on every future use) and registers the PUBLIC key with the
     * backend against [userId]. The private key never leaves the Keystore.
     * Call this only after the UI layer has already confirmed the user's
     * intent via BiometricPrompt — this method does not show any prompt.
     * [email] is cached locally only so a logged-out password-reset flow can
     * find which Keystore alias belongs to a typed-in email address; it is
     * never used as a trust decision by itself.
     */
    suspend fun registerDevice(userId: String, email: String, deviceLabel: String): Result<Unit>

    /** Removes the local key + local trust record (e.g. on logout/invalidation). */
    suspend fun forgetDevice(userId: String)

    /** Local-only lookup used while logged out: which Keystore alias (userId) belongs to [email] on this device, if any. */
    suspend fun getLocalUserIdForEmail(email: String): String?

    /**
     * Every account this device already has a key for, so forgot-password
     * never has to ask for an email up front (see TrustedAccountSummary).
     */
    suspend fun getAllTrustedAccounts(): List<TrustedAccountSummary>

    /**
     * Step 1 of password-reset device verification: asks the backend if this
     * device is registered/trusted for [email] and, if so, returns a
     * one-time challenge to sign.
     */
    suspend fun checkDeviceTrust(email: String): Result<DeviceTrustCheck>

    /**
     * Step 2: sends a signature the UI layer already produced (by having the
     * user authenticate against the Keystore-backed key via BiometricPrompt's
     * CryptoObject — see DeviceCredentialManager) along with the new
     * password. The backend alone decides whether the signature is valid
     * for the stored public key before it will reset the password; a local
     * "biometric succeeded" flag is never treated as sufficient on its own.
     */
    suspend fun verifyDeviceAndResetPassword(
        email: String,
        deviceId: String,
        challenge: String,
        signatureBase64: String,
        newPassword: String
    ): Result<DeviceVerificationResult>

    /**
     * Signature-only variant of [verifyDeviceAndResetPassword] used by the
     * forgot-password flow: no password is sent or changed, a successful
     * signature just returns a Firebase sign-in token.
     */
    suspend fun verifyDeviceAndLogin(
        email: String,
        deviceId: String,
        challenge: String,
        signatureBase64: String
    ): Result<DeviceLoginResult>

    /** Fallback recovery path when this device is not a trusted device. */
    suspend fun sendFallbackResetEmail(email: String): Result<Unit>
}

interface SavedSetupRepository {
    fun getSetupsForUser(userId: String): Flow<List<SavedSetup>>
    suspend fun saveCurrentSetup(
        userId: String,
        title: String,
        description: String,
        previewWallpaper: String,
        iconPackName: String,
        gridRows: Int,
        gridCols: Int,
        items: List<HomeItem>
    ): Long
    suspend fun applySavedSetup(setupId: Long, homeRepository: HomeRepository): Boolean
    suspend fun deleteSetup(setup: SavedSetup)
    suspend fun toggleSetupFavorite(setupId: Long, isFavorite: Boolean)
    fun getFavoritesForUser(userId: String): Flow<List<FavoriteItem>>
    suspend fun toggleFavorite(userId: String, type: FavoriteType, targetId: String, title: String, previewUrl: String): Boolean
}

interface SettingsRepository {
    val settingsFlow: Flow<LauncherSettings>
    suspend fun updateGridDimensions(rows: Int, cols: Int)
    suspend fun updateIconScale(scale: Float)
    suspend fun toggleLabels(show: Boolean)
    suspend fun toggleDarkTheme(dark: Boolean)
    fun isDefaultLauncher(): Boolean
    fun requestDefaultLauncher()

    // New persistence for customization selections
    suspend fun saveSelectedIconPack(name: String)
    suspend fun saveSelectedTheme(themeId: String, primaryColor: String, secondaryColor: String)
    suspend fun saveSelectedFont(fontName: String)
}
