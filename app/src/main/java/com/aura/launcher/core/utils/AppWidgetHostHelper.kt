package com.aura.launcher.core.utils

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetHostView
import com.aura.launcher.customization.widgets.AuraWidgetInfo
import com.aura.launcher.customization.widgets.WidgetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One instance lives for the whole app (see AuraLauncherApp).
 * startListening()/stopListening() must bind to Activity lifecycle (see MainActivity.onStart/onStop),
 * otherwise widgets won't receive live updates.
 */
class AppWidgetHostHelper(
    private val context: Context,
    hostId: Int = 42
) {
    private val appWidgetHost: AppWidgetHost = AppWidgetHost(context, hostId)
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)

    fun startListening() = appWidgetHost.startListening()
    fun stopListening() = appWidgetHost.stopListening()

    /** All real widgets installed on the device, for the picker sheet. */
    suspend fun getAvailableWidgets(): List<AuraWidgetInfo> = withContext(Dispatchers.IO) {
        appWidgetManager.installedProviders.map { providerInfo ->
            val provider = providerInfo.provider
            val label = try {
                providerInfo.loadLabel(context.packageManager)
            } catch (e: Exception) {
                provider.packageName
            }
            AuraWidgetInfo(
                type = WidgetType.SYSTEM_WIDGET,
                title = label,
                description = provider.packageName,
                spanX = cellSpanFromPx(providerInfo.minWidth),
                spanY = cellSpanFromPx(providerInfo.minHeight),
                category = "Widget",
                componentName = provider
            )
        }
    }

    suspend fun allocateAppWidgetId(): Int = withContext(Dispatchers.IO) {
        appWidgetHost.allocateAppWidgetId()
    }

    /** Default launcher automatically gets bind permission, so this normally returns true directly. */
    fun bindWidgetIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean {
        return appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
    }

    fun requiresConfiguration(provider: ComponentName): Boolean {
        return getProviderInfo(provider)?.configure != null
    }

    fun createConfigurationIntent(appWidgetId: Int, provider: ComponentName): Intent? {
        val configComponent = getProviderInfo(provider)?.configure ?: return null
        return Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configComponent
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    /** Inflates the live widget view for an already-bound appWidgetId. */
    fun createHostView(appWidgetId: Int, provider: ComponentName): AppWidgetHostView? {
        val providerInfo = getProviderInfo(provider) ?: return null
        return appWidgetHost.createView(context, appWidgetId, providerInfo)
    }

    fun deleteAppWidgetId(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
    }

    private fun getProviderInfo(provider: ComponentName): AppWidgetProviderInfo? {
        return appWidgetManager.installedProviders.firstOrNull { it.provider == provider }
    }

    private fun cellSpanFromPx(minPx: Int): Int {
        val dp = minPx / context.resources.displayMetrics.density
        return (dp / 70).toInt().coerceIn(1, 4)
    }
}