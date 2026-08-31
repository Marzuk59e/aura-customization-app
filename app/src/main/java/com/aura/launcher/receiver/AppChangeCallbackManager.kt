package com.aura.launcher.receiver

import android.content.pm.LauncherApps
import com.aura.launcher.AuraLauncherApp
import com.aura.launcher.core.utils.ChangeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manages app change detection using LauncherApps.Callback instead of
 * the old static BroadcastReceiver.
 *
 * LauncherApps.Callback advantages:
 * - No need for Manifest <receiver> registration
 * - Handles work profiles automatically
 * - More reliable than PACKAGE_ADDED/REMOVED broadcasts
 * - Lifecycle-aware when registered/unregistered in Activity
 */
class AppChangeCallbackManager(private val app: AuraLauncherApp) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var registeredCallback: LauncherApps.Callback? = null

    /**
     * Start listening for app install/uninstall/update events.
     * Call this from MainActivity.onCreate() or Application.onCreate().
     */
    fun startListening() {
        if (registeredCallback != null) return // Already registered

        registeredCallback = app.packageManagerHelper.registerAppChangeCallback { packageName, changeType ->
            scope.launch {
                when (changeType) {
                    ChangeType.REMOVED -> {
                        app.appRepository.removeAppByPackage(packageName)
                    }
                    ChangeType.ADDED, ChangeType.CHANGED -> {
                        app.appRepository.refreshInstalledApps()
                    }
                }
            }
        }
    }

    /**
     * Stop listening. Call from onDestroy or when no longer needed.
     */
    fun stopListening() {
        registeredCallback?.let {
            app.packageManagerHelper.unregisterAppChangeCallback(it)
            registeredCallback = null
        }
    }
}
