package com.aura.launcher.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WallpaperDto(
    val id: String,
    val title: String,
    val imageUrl: String,
    val thumbnailUrl: String? = null,
    val categoryId: String? = null,
    val orientation: String = "portrait",
    val tier: String = "free",
    val status: String = "published",
    val palette: List<String>? = null,
    val tags: List<String>? = null,
    val downloads: Int = 0
)

data class IconPackDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val iconCount: Int = 0,
    val previewUrl: String? = null,
    val coverUrl: String? = null,
    val tier: String = "free",
    val status: String = "published",
    val author: String = "Aura Studio"
)

data class IconMappingDto(
    val id: String? = null,
    val name: String,
    val componentName: String,
    val imageUrl: String,
    val format: String = "webp"
)

data class ThemeDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val iconPackId: String? = null,
    val wallpaperId: String? = null,
    val primaryColor: String = "#7000FF",
    val secondaryColor: String = "#00F2FE",
    val textColor: String = "#FFFFFF",
    val backgroundColor: String = "#0A0A0E",
    val previewUrl: String? = null,
    val tier: String = "free",
    val status: String = "published"
)

data class WidgetDto(
    val id: String,
    val name: String,
    val kind: String = "clock",
    val defaultSize: String = "4x2",
    val previewUrl: String? = null,
    val tier: String = "free"
)

data class AppVersionDto(
    val version: String,
    val versionCode: Int,
    val channel: String = "production",
    val minSupportedVersion: String = "1.0.0",
    val forceUpdate: Boolean = false,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null
)

data class RemoteConfigDto(
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String? = null,
    val featuredThemeId: String? = null,
    val featuredWallpaperId: String? = null,
    val apiRateLimit: Int = 60,
    val contentCacheTtl: Int = 3600
)

data class CategoryDto(
    val id: String,
    val name: String,
    val scope: String = "wallpaper",
    val itemCount: Int = 0
)

data class PublicWidgetDto(
    val id: String,
    val name: String,
    val type: String = "clock",
    val price: Double = 0.0,
    val isFree: Boolean = true,
    val previewUrl: String? = null,
    val version: String = "1.0.0"
)

data class PublicHomeDto(
    val featured: List<String>? = null,
    val newProducts: List<String>? = null,
    val popularProducts: List<String>? = null,
    val categories: List<CategoryDto>? = null
)

// Mirrors AdminAccount / User profile returned by GET /auth/me on the PHP
// backend (aura-launcher.unaux.com). The backend owns this document in
// Firestore — the app never writes user profile fields directly.
data class UserProfileDto(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
    val role: String? = null,
    val status: String? = null,
    val lastLoginAt: String? = null
)

// ---------------------------------------------------------------------
// Secure device authentication (password-reset device verification).
// See DeviceAuthApi / DeviceAuthRepository. The backend never receives a
// private key, a fingerprint/face template, or a PIN — only a public key
// (at registration) and a signature over a one-time server challenge (at
// verification), which it checks against the stored public key.
// ---------------------------------------------------------------------

/** POST auth/devices/register — sent right after a normal Firebase login. */
data class DeviceRegisterRequestDto(
    val deviceId: String,
    val publicKey: String,       // Base64 X.509 SubjectPublicKeyInfo
    val deviceLabel: String,     // e.g. "Pixel 8 Pro"
    val algorithm: String = "SHA256withECDSA"
)

data class DeviceRegisterResponseDto(
    val registered: Boolean,
    val deviceId: String
)

/** POST auth/devices/challenge — first step of password-reset device verification. */
data class DeviceChallengeRequestDto(
    val email: String,
    val deviceId: String
)

data class DeviceChallengeResponseDto(
    val deviceTrusted: Boolean,
    val deviceId: String? = null,
    val challenge: String? = null,   // one-time random string to sign, null if not trusted
    val expiresInSeconds: Int? = null
)

/** POST auth/devices/verify-reset — second step: signed challenge + new password. */
data class DeviceVerifyResetRequestDto(
    val email: String,
    val deviceId: String,
    val challenge: String,
    val signature: String,       // Base64 signature over `challenge`, produced by the Keystore key
    val newPassword: String
)

data class DeviceVerifyResetResponseDto(
    val success: Boolean,
    val message: String? = null,
    // Optional Firebase custom token so the app can sign the user straight
    // back in after a successful device-verified reset, without asking them
    // to retype the password they just set.
    val signInToken: String? = null
)

/**
 * POST auth/devices/verify-login — lighter sibling of verify-reset used by
 * the forgot-password flow now that a successful verification signs the
 * user straight in. Same signature check, no password field at all.
 */
data class DeviceVerifyLoginRequestDto(
    val email: String,
    val deviceId: String,
    val challenge: String,
    val signature: String
)

data class DeviceVerifyLoginResponseDto(
    val success: Boolean,
    val message: String? = null,
    val signInToken: String? = null
)

