package com.aura.launcher.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aura.launcher.domain.model.LauncherSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_settings")
val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_session")

data class SelectedCustomizationTheme(
    val themeId: String = "",
    val primaryColor: String = "#7000FF",
    val secondaryColor: String = "#00F2FE",
    val fontName: String = "Default"
)

class LauncherPreferences(private val context: Context) {
    companion object {
        val GRID_ROWS = intPreferencesKey("grid_rows")
        val GRID_COLS = intPreferencesKey("grid_cols")
        val DOCK_MAX = intPreferencesKey("dock_max_items")
        val ICON_SCALE = floatPreferencesKey("icon_scale")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val IS_INITIALIZED = booleanPreferencesKey("is_initialized")
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")

        // New customization persistence keys
        val SELECTED_ICON_PACK = stringPreferencesKey("selected_icon_pack")
        val SELECTED_THEME_ID = stringPreferencesKey("selected_theme_id")
        val SELECTED_THEME_PRIMARY = stringPreferencesKey("selected_theme_primary")
        val SELECTED_THEME_SECONDARY = stringPreferencesKey("selected_theme_secondary")
        val SELECTED_FONT = stringPreferencesKey("selected_font")
    }

    val settingsFlow: Flow<LauncherSettings> = context.launcherDataStore.data.map { prefs ->
        LauncherSettings(
            gridRows = prefs[GRID_ROWS] ?: 5,
            gridCols = prefs[GRID_COLS] ?: 4,
            dockMaxItems = prefs[DOCK_MAX] ?: 5,
            iconScale = prefs[ICON_SCALE] ?: 1.0f,
            showLabels = prefs[SHOW_LABELS] ?: true,
            darkTheme = prefs[DARK_THEME] ?: true
        )
    }

    val customizationFlow: Flow<SelectedCustomizationTheme> = context.launcherDataStore.data.map { prefs ->
        SelectedCustomizationTheme(
            themeId = prefs[SELECTED_THEME_ID] ?: "",
            primaryColor = prefs[SELECTED_THEME_PRIMARY] ?: "#7000FF",
            secondaryColor = prefs[SELECTED_THEME_SECONDARY] ?: "#00F2FE",
            fontName = prefs[SELECTED_FONT] ?: "Default"
        )
    }

    val isInitializedFlow: Flow<Boolean> = context.launcherDataStore.data.map { prefs ->
        prefs[IS_INITIALIZED] ?: false
    }

    val hasSeenWelcomeFlow: Flow<Boolean> = context.launcherDataStore.data.map { prefs ->
        prefs[HAS_SEEN_WELCOME] ?: false
    }

    suspend fun updateGrid(rows: Int, cols: Int) {
        context.launcherDataStore.edit { prefs ->
            prefs[GRID_ROWS] = rows
            prefs[GRID_COLS] = cols
        }
    }

    suspend fun updateIconScale(scale: Float) {
        context.launcherDataStore.edit { prefs ->
            prefs[ICON_SCALE] = scale
        }
    }

    suspend fun updateShowLabels(show: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[SHOW_LABELS] = show
        }
    }

    suspend fun updateTheme(dark: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[DARK_THEME] = dark
        }
    }

    // Save selected icon pack name
    suspend fun setSelectedIconPack(name: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[SELECTED_ICON_PACK] = name
        }
    }

    // Save selected theme info
    suspend fun setSelectedTheme(themeId: String, primaryColor: String, secondaryColor: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[SELECTED_THEME_ID] = themeId
            prefs[SELECTED_THEME_PRIMARY] = primaryColor
            prefs[SELECTED_THEME_SECONDARY] = secondaryColor
        }
    }

    // Save selected font
    suspend fun setSelectedFont(fontName: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[SELECTED_FONT] = fontName
        }
    }

    suspend fun setInitialized(initialized: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[IS_INITIALIZED] = initialized
        }
    }

    suspend fun setHasSeenWelcome(seen: Boolean) {
        context.launcherDataStore.edit { prefs ->
            prefs[HAS_SEEN_WELCOME] = seen
        }
    }
}

class AuthPreferences(private val context: Context) {
    companion object {
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val IS_GUEST = booleanPreferencesKey("is_guest")
    }

    val activeUserIdFlow: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[ACTIVE_USER_ID]
    }

    val isGuestFlow: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[IS_GUEST] ?: true
    }

    suspend fun saveSession(userId: String, isGuest: Boolean) {
        context.authDataStore.edit { prefs ->
            prefs[ACTIVE_USER_ID] = userId
            prefs[IS_GUEST] = isGuest
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { prefs ->
            prefs.remove(ACTIVE_USER_ID)
            prefs[IS_GUEST] = true
        }
    }
}
