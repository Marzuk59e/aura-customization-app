package com.aura.launcher.core.utils

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aura.launcher.AuraLauncherApp

/**
 * Loads an installed app's icon as a [Bitmap], via [PackageManagerHelper].
 *
 * Used by every icon tile in the launcher (home grid, dock, app drawer,
 * folder contents) — previously each of those places copy-pasted this same
 * context -> appInstance -> getAppIconDrawable -> drawableToBitmap lookup
 * independently. Pulling it into one place means a future icon-loading
 * change (caching, fallback icon, etc.) only needs to happen once.
 *
 * Returns null while [packageName] is null, or if the icon can't be loaded
 * (app uninstalled, no matching activity, etc.) — callers already have a
 * fallback (a letter avatar) for that case.
 */
@Composable
fun rememberAppIconBitmap(packageName: String?, activityName: String?): Bitmap? {
    val context = LocalContext.current
    val appInstance = remember(context) {
        context.applicationContext as? AuraLauncherApp ?: AuraLauncherApp.instance
    }
    return remember(packageName, activityName) {
        if (packageName != null) {
            val drawable = appInstance.packageManagerHelper.getAppIconDrawable(packageName, activityName ?: "")
            drawable?.let { appInstance.packageManagerHelper.drawableToBitmap(it) }
        } else {
            null
        }
    }
}
