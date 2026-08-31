package com.aura.launcher.core.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat
import com.aura.launcher.core.datastore.LauncherPreferences
import com.aura.launcher.core.datastore.SelectedCustomizationTheme

private val DarkColorScheme = darkColorScheme(
    primary = AuraPurpleLight,
    onPrimary = TextPrimary,
    secondary = AuraCyan,
    tertiary = AuraPink,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = AuraPurple,
    onPrimary = LightSurface,
    secondary = AuraCyan,
    tertiary = AuraPink,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = DarkBg,
    onSurface = DarkBg,
    outline = LightBorder
)

@Composable
fun AuraLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val launcherPreferences = remember(context) { LauncherPreferences(context.applicationContext) }
    val customization by launcherPreferences.customizationFlow.collectAsState(initial = SelectedCustomizationTheme())

    val primaryColor = remember(customization.primaryColor) {
        try {
            Color(AndroidColor.parseColor(customization.primaryColor))
        } catch (_: Exception) {
            AuraPurple
        }
    }
    val secondaryColor = remember(customization.secondaryColor) {
        try {
            Color(AndroidColor.parseColor(customization.secondaryColor))
        } catch (_: Exception) {
            AuraCyan
        }
    }
    val selectedFontFamily = remember(customization.fontName) {
        when (customization.fontName.lowercase()) {
            "inter" -> FontFamily.SansSerif
            "roboto" -> FontFamily.SansSerif
            "sf pro" -> FontFamily.SansSerif
            else -> FontFamily.Default
        }
    }

    LaunchedEffect(primaryColor, secondaryColor, darkTheme) {
        applyThemePalette(primary = primaryColor, secondary = secondaryColor, darkMode = darkTheme)
    }
    LaunchedEffect(selectedFontFamily) {
        applyTypography(selectedFontFamily)
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = TextPrimary,
            secondary = secondaryColor,
            tertiary = AuraPink,
            background = DarkBg,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            outline = DarkBorder
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            secondary = secondaryColor,
            tertiary = AuraPink,
            background = LightBg,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            onBackground = DarkBg,
            onSurface = DarkBg,
            outline = LightBorder
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuraTypography,
        content = content
    )
}
