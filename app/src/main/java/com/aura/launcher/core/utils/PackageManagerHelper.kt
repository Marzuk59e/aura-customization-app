package com.aura.launcher.core.utils

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import com.aura.launcher.domain.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Upgraded PackageManagerHelper using LauncherApps API (Android 5.0+).
 *
 * Why LauncherApps instead of PackageManager.queryIntentActivities():
 * - LauncherApps is the official launcher API, designed specifically for home screen replacements
 * - It supports work profiles (managed users) out of the box
 * - It provides a Callback mechanism for real-time app install/remove/change events
 *   (replacing the old static BroadcastReceiver approach)
 * - It returns proper launcher-specific icons and labels
 */
class PackageManagerHelper(private val context: Context) {

    private val launcherApps: LauncherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val userManager: UserManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    // ──────────────────────────────────────────────
    // App Discovery via LauncherApps API
    // ──────────────────────────────────────────────

    /**
     * Fetches all launchable apps using LauncherApps.getActivityList().
     * This is the modern, recommended approach for launchers.
     */
    suspend fun getInstalledLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AppInfo>()
        val ownPackage = context.packageName

        for (userHandle in userManager.userProfiles) {
            val activities: List<LauncherActivityInfo> = launcherApps.getActivityList(null, userHandle)

            for (activityInfo in activities) {
                val packageName = activityInfo.componentName.packageName
                val activityName = activityInfo.componentName.className

                // Exclude our own launcher from the app list
                if (packageName == ownPackage) continue

                val label = activityInfo.label?.toString() ?: packageName
                val installTime = try {
                    context.packageManager.getPackageInfo(packageName, 0).firstInstallTime
                } catch (e: Exception) {
                    0L
                }

                result.add(
                    AppInfo(
                        packageName = packageName,
                        activityName = activityName,
                        label = label,
                        category = resolveCategory(activityInfo),
                        isHidden = false,
                        installTime = installTime
                    )
                )
            }
        }

        result.sortedBy { it.label.lowercase() }
    }

    // ──────────────────────────────────────────────
    // App Icon Loading (Real System Icons)
    // ──────────────────────────────────────────────

    /**
     * Returns the real app icon Drawable using LauncherApps API.
     * Falls back to PackageManager if LauncherApps fails.
     */
    fun getAppIconDrawable(packageName: String, activityName: String): Drawable? {
        return try {
            val component = ComponentName(packageName, activityName)
            val userHandle = android.os.Process.myUserHandle()
            val activityInfoList = launcherApps.getActivityList(packageName, userHandle)
            val matchingActivity = activityInfoList.firstOrNull {
                it.componentName == component
            }
            if (matchingActivity != null) {
                // Get high-density icon from LauncherApps (respects adaptive icons)
                matchingActivity.getIcon(context.resources.displayMetrics.densityDpi)
            } else {
                // Fallback: try PackageManager directly
                context.packageManager.getActivityIcon(component)
            }
        } catch (e: Exception) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (ex: Exception) {
                Log.w("PackageManagerHelper", "Failed to load icon for $packageName", ex)
                null
            }
        }
    }

    /**
     * Converts a Drawable to a Bitmap for use with Compose Image.
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 108
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 108
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // ──────────────────────────────────────────────
    // App Launching
    // ──────────────────────────────────────────────

    fun launchApp(packageName: String, activityName: String? = null): Boolean {
        return try {
            if (activityName != null) {
                val component = ComponentName(packageName, activityName)
                val userHandle = android.os.Process.myUserHandle()
                // Use LauncherApps.startMainActivity for proper launcher behavior
                launcherApps.startMainActivity(component, userHandle, null, null)
                true
            } else {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (intent != null) {
                    context.startActivity(intent)
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("PackageManagerHelper", "Failed to launch $packageName", e)
            false
        }
    }

    // ──────────────────────────────────────────────
    // LauncherApps.Callback Registration
    // ──────────────────────────────────────────────

    /**
     * Registers a LauncherApps.Callback for real-time app change detection.
     * This replaces the static BroadcastReceiver pattern.
     *
     * @param onAppChanged Called when any app is added, removed, or changed.
     *                     The String parameter is the package name.
     */
    fun registerAppChangeCallback(onAppChanged: (String, ChangeType) -> Unit): LauncherApps.Callback {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                onAppChanged(packageName, ChangeType.ADDED)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                onAppChanged(packageName, ChangeType.CHANGED)
            }

            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                onAppChanged(packageName, ChangeType.REMOVED)
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) {
                packageNames.forEach { onAppChanged(it, ChangeType.ADDED) }
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean
            ) {
                if (!replacing) {
                    packageNames.forEach { onAppChanged(it, ChangeType.REMOVED) }
                }
            }
        }

        launcherApps.registerCallback(callback)
        return callback
    }

    /**
     * Unregisters a previously registered callback.
     */
    fun unregisterAppChangeCallback(callback: LauncherApps.Callback) {
        launcherApps.unregisterCallback(callback)
    }

    // ──────────────────────────────────────────────
    // Default Launcher Detection & Request
    // ──────────────────────────────────────────────

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun openDefaultLauncherSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            }
        }
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }

    /**
     * Expand the system status bar (notification panel).
     * Uses reflection on StatusBarManager for compatibility.
     */
    @Suppress("WrongConstant")
    fun expandNotificationPanel() {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarClass = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarClass.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) {
            Log.w("PackageManagerHelper", "Cannot expand notification panel", e)
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private fun resolveCategory(activityInfo: LauncherActivityInfo): String {
        return try {
            val appInfo = activityInfo.applicationInfo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (appInfo.category) {
                    android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Games"
                    android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "Media"
                    android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "Media"
                    android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Media"
                    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                    android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "News"
                    android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "Tools"
                    android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                    else -> "Apps"
                }
            } else {
                "Apps"
            }
        } catch (e: Exception) {
            "Apps"
        }
    }
}

enum class ChangeType {
    ADDED, REMOVED, CHANGED
}
