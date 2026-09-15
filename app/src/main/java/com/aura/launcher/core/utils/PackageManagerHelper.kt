package com.aura.launcher.core.utils

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import com.aura.launcher.domain.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.pm.LauncherApps
import android.os.UserHandle

class PackageManagerHelper(private val context: Context) {

        private val launcherApps: LauncherApps by lazy {
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    /** Registers a live callback for app install/uninstall/update events via LauncherApps. */
    fun registerAppChangeCallback(onChange: (packageName: String, changeType: ChangeType) -> Unit): LauncherApps.Callback {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) {
                onChange(packageName, ChangeType.ADDED)
            }
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                onChange(packageName, ChangeType.REMOVED)
            }
            override fun onPackageChanged(packageName: String, user: UserHandle) {
                onChange(packageName, ChangeType.CHANGED)
            }
            override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) {
                packageNames.forEach { onChange(it, ChangeType.CHANGED) }
            }
            override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) {
                packageNames.forEach { onChange(it, ChangeType.CHANGED) }
            }
        }
        launcherApps.registerCallback(callback)
        return callback
    }

    /** Unregisters a callback previously returned by registerAppChangeCallback. */
    fun unregisterAppChangeCallback(callback: LauncherApps.Callback) {
        launcherApps.unregisterCallback(callback)
    }

    suspend fun getInstalledLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            packageManager.queryIntentActivities(mainIntent, 0)
        }

        resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName
            val activityName = activityInfo.name

            // Exclude our own launcher from app drawer
            if (packageName == context.packageName) return@mapNotNull null

            val label = resolveInfo.loadLabel(packageManager).toString()
            val installTime = try {
                packageManager.getPackageInfo(packageName, 0).firstInstallTime
            } catch (e: Exception) {
                0L
            }

            AppInfo(
                packageName = packageName,
                activityName = activityName,
                label = label,
                category = resolveCategory(resolveInfo),
                isHidden = false,
                installTime = installTime
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun getAppIconDrawable(packageName: String, activityName: String): Drawable? {
        return try {
            val component = ComponentName(packageName, activityName)
            context.packageManager.getActivityIcon(component)
        } catch (e: Exception) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (ex: Exception) {
                null
            }
        }
    }

    /**
     * Converts a Drawable to a Bitmap for Compose Image rendering.
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

    /**
     * Expands the Android status bar / notification panel.
     */
    @Suppress("WrongConstant")
    fun expandNotificationPanel() {
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarClass = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarClass.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun launchApp(packageName: String, activityName: String? = null): Boolean {
        return try {
            val intent = if (activityName != null) {
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = ComponentName(packageName, activityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

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

    private fun resolveCategory(resolveInfo: ResolveInfo): String {
        return "Apps"
    }
}

      enum class ChangeType { ADDED, REMOVED, CHANGED }