package com.aura.launcher.domain.model

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: String = "General",
    val isHidden: Boolean = false,
    val installTime: Long = 0L,
    val launchCount: Int = 0
) {
    val componentKey: String
        get() = "$packageName/$activityName"
}

enum class HomeItemType {
    APP,
    SHORTCUT,
    FOLDER,
    WIDGET
}

data class HomeItem(
    val id: Long = 0,
    val pageIndex: Int, // 0 = First Page, -1 = Dock
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val itemType: HomeItemType = HomeItemType.APP,
    val packageName: String? = null,
    val activityName: String? = null,
    val label: String? = null,
    val iconUri: String? = null,
    val folderId: Long? = null,
    val widgetId: Int? = null,
    val widgetProvider: String? = null
)

data class Folder(
    val id: Long = 0,
    val title: String,
    val color: String = "#7000FF",
    val apps: List<AppInfo> = emptyList()
)

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isGuest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class SavedSetup(
    val id: Long = 0,
    val userId: String,
    val title: String,
    val description: String = "",
    val previewWallpaper: String = "",
    val iconPackName: String = "Default Modern",
    val gridRows: Int = 5,
    val gridCols: Int = 4,
    val layoutJson: String = "", // serialized HomeItem list
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

enum class FavoriteType {
    THEME,
    WALLPAPER,
    ICON_PACK,
    SETUP
}

data class FavoriteItem(
    val id: Long = 0,
    val userId: String,
    val type: FavoriteType,
    val targetId: String,
    val title: String,
    val previewUrl: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

data class LauncherSettings(
    val gridRows: Int = 5,
    val gridCols: Int = 4,
    val dockMaxItems: Int = 5,
    val iconScale: Float = 1.0f,
    val showLabels: Boolean = true,
    val darkTheme: Boolean = true,
    val isDefaultLauncher: Boolean = false
)

// ---------------------------------------------------------------------
// Secure device authentication (password-reset device verification).
// Replaces the old simulated OTP flow with a real Keystore-backed,
// backend-verified challenge/response — see DeviceAuthRepository.
// ---------------------------------------------------------------------

/** What this device is currently capable of, from BiometricManager. */
enum class DeviceSecurityCapability {
    BIOMETRIC_AVAILABLE,     // Class 3 (strong) biometric enrolled and ready
    DEVICE_CREDENTIAL_ONLY,  // No biometric, but a PIN/pattern/password is set
    NONE_ENROLLED,           // Hardware exists but nothing is enrolled yet
    NOT_SUPPORTED            // No secure hardware on this device at all
}

/** Result of asking the backend whether THIS device is trusted for an account. */
sealed interface DeviceTrustCheck {
    data class Trusted(val deviceId: String, val challenge: String) : DeviceTrustCheck
    data object NotTrusted : DeviceTrustCheck
}

/** Outcome of a completed device-authentication + backend verification attempt. */
sealed interface DeviceVerificationResult {
    data class Success(val sessionToken: String?) : DeviceVerificationResult
    data class Failed(val message: String) : DeviceVerificationResult
}

/**
 * One locally-registered account on this device (from TrustedDeviceEntity).
 * Used so forgot-password never has to ask for an email up front — if this
 * list has exactly one entry it's used automatically; if it has more than
 * one (rare: several accounts have used this device), the UI shows a small
 * picker built from these summaries instead of a text field.
 */
data class TrustedAccountSummary(
    val userId: String,
    val email: String,
    val deviceLabel: String
)

/** Outcome of a signature-only device verification that signs the user in directly (no password change). */
sealed interface DeviceLoginResult {
    data class Success(val signInToken: String) : DeviceLoginResult
    data class Failed(val message: String) : DeviceLoginResult
}
