package com.aura.launcher.launcher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * AuraAppWidgetHost manages native Android AppWidget lifecycle:
 * - Allocating widget IDs
 * - Binding widget providers
 * - Hosting native widget views (Clock, Weather, Spotify, Notes, etc.)
 */
class AuraAppWidgetHost(private val context: Context) {

    companion object {
        const val HOST_ID = 2048
        const val REQUEST_PICK_APPWIDGET = 101
        const val REQUEST_BIND_APPWIDGET = 102
    }

    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(context.applicationContext, HOST_ID)

    fun startListening() {
        try {
            appWidgetHost.startListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun deleteAppWidgetId(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
    }

    fun createWidgetView(appWidgetId: Int, providerInfo: AppWidgetProviderInfo? = null, activityContext: Context? = null): View? {
        val targetContext = activityContext ?: context
        val info = providerInfo ?: appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return null
        return try {
            appWidgetHost.createView(targetContext, appWidgetId, info)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun launchWidgetPicker(activity: ComponentActivity) {
        val appWidgetId = allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        activity.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET)
    }
}

/**
 * Composable wrapper to render a native Android AppWidgetView inside Jetpack Compose
 */
@Composable
fun AndroidAppWidgetHostView(
    appWidgetId: Int,
    host: AuraAppWidgetHost,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            host.createWidgetView(appWidgetId = appWidgetId, activityContext = ctx) ?: View(ctx)
        },
        modifier = modifier
    )
}
