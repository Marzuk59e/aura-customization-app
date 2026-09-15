package com.aura.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aura.launcher.AuraLauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return

        val app = context.applicationContext as? AuraLauncherApp ?: AuraLauncherApp.instance

        CoroutineScope(Dispatchers.IO).launch {
            when (action) {
                Intent.ACTION_PACKAGE_REMOVED -> {
                    val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (!replacing) {
                        app.appRepository.removeAppByPackage(packageName)
                    }
                }
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_CHANGED -> {
                    app.appRepository.refreshInstalledApps()
                }
            }
        }
    }
}
