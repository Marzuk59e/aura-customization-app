package com.aura.launcher.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Matches css/base/variables.css from the aura-customization-app web design.
// These are backed by Compose State (`by mutableStateOf`) instead of plain `var`s
// so that any Composable reading them automatically recomposes when a preset,
// mood, or theme changes them — matching the reactive behaviour of the CSS
// custom-property version on the web build.
var AuraPurple by mutableStateOf(Color(0xFFA855F7))      // --accent-tertiary
var AuraPurpleLight by mutableStateOf(Color(0xFFC084FC))
var AuraCyan by mutableStateOf(Color(0xFF00E5FF))        // --accent-secondary
var AuraPink by mutableStateOf(Color(0xFFFF0055))        // --accent-primary
var AuraAmber by mutableStateOf(Color(0xFFF59E0B))       // --color-warning
var AuraSuccess by mutableStateOf(Color(0xFF10B981))     // --color-success
var AuraDanger by mutableStateOf(Color(0xFFFF4D6D))      // --color-danger (validation / error text)

var DarkBg by mutableStateOf(Color(0xFF05070C))          // --bg-app
var DarkSurface by mutableStateOf(Color(0xFF10131C))     // --bg-card
var DarkSurfaceGlass by mutableStateOf(Color(0xE010131C)) // --bg-glass
var DarkSurfaceVariant by mutableStateOf(Color(0xFF181D2A)) // --bg-card-elevated
var DarkBorder by mutableStateOf(Color(0x14FFFFFF))      // --border-subtle

var TextPrimary by mutableStateOf(Color(0xFFF8FAFC))     // --text-main
var TextSecondary by mutableStateOf(Color(0xFF94A3B8))   // --text-sub
var TextMuted by mutableStateOf(Color(0xFF64748B))       // --text-muted

var LightBg by mutableStateOf(Color(0xFFF7F8FC))
var LightSurface by mutableStateOf(Color(0xFFFFFFFF))
var LightSurfaceGlass by mutableStateOf(Color(0xCCFFFFFF))
var LightSurfaceVariant by mutableStateOf(Color(0xFFEAEBF2))
var LightBorder by mutableStateOf(Color(0x1A000000))

fun resetDefaultPalette() {
    AuraPurple = Color(0xFFA855F7)
    AuraPurpleLight = Color(0xFFC084FC)
    AuraCyan = Color(0xFF00E5FF)
    AuraPink = Color(0xFFFF0055)
    AuraAmber = Color(0xFFF59E0B)
    AuraSuccess = Color(0xFF10B981)
    AuraDanger = Color(0xFFFF4D6D)

    DarkBg = Color(0xFF05070C)
    DarkSurface = Color(0xFF10131C)
    DarkSurfaceGlass = Color(0xE010131C)
    DarkSurfaceVariant = Color(0xFF181D2A)
    DarkBorder = Color(0x14FFFFFF)

    TextPrimary = Color(0xFFF8FAFC)
    TextSecondary = Color(0xFF94A3B8)
    TextMuted = Color(0xFF64748B)

    LightBg = Color(0xFFF7F8FC)
    LightSurface = Color(0xFFFFFFFF)
    LightSurfaceGlass = Color(0xCCFFFFFF)
    LightSurfaceVariant = Color(0xFFEAEBF2)
    LightBorder = Color(0x1A000000)
}

fun applyThemePalette(
    primary: Color = AuraPurple,
    secondary: Color = AuraCyan,
    darkMode: Boolean = true
) {
    // Matches web: only --accent-primary & --accent-secondary change per
    // preset. --accent-tertiary (purple) stays constant, so AuraPurple is
    // intentionally NOT reassigned here anymore.
    AuraPink = primary
    AuraCyan = secondary

    if (darkMode) {
        DarkBg = Color(0xFF05070C)
        DarkSurface = Color(0xFF10131C)
        DarkSurfaceGlass = Color(0xE010131C)
        DarkSurfaceVariant = Color(0xFF181D2A)
        DarkBorder = Color(0x14FFFFFF)
        TextPrimary = Color(0xFFF8FAFC)
        TextSecondary = Color(0xFF94A3B8)
        TextMuted = Color(0xFF64748B)
        LightBg = Color(0xFFF7F8FC)
        LightSurface = Color(0xFFFFFFFF)
        LightSurfaceGlass = Color(0xCCFFFFFF)
        LightSurfaceVariant = Color(0xFFEAEBF2)
        LightBorder = Color(0x1A000000)
    } else {
        LightBg = Color(0xFFF7F8FC)
        LightSurface = Color(0xFFFFFFFF)
        LightSurfaceGlass = Color(0xCCFFFFFF)
        LightSurfaceVariant = Color(0xFFEAEBF2)
        LightBorder = Color(0x1A000000)
        TextPrimary = Color(0xFF111827)
        TextSecondary = Color(0xFF475569)
        TextMuted = Color(0xFF64748B)
        DarkBg = Color(0xFF05070C)
        DarkSurface = Color(0xFF10131C)
        DarkSurfaceGlass = Color(0xE010131C)
        DarkSurfaceVariant = Color(0xFF181D2A)
        DarkBorder = Color(0x14FFFFFF)
    }
}

// Applied on top of the normal theme palette. "standard" is a no-op (keeps
// whatever theme/vibe-sync colors are already active); the other two presets
// override accents and/or contrast for accessibility.
fun applyAccessibilityPreset(preset: String) {
    when (preset) {
        "deuteranopia" -> {
            // Cobalt blue + warm amber/gold — avoids red/green confusion.
            AuraPink = Color(0xFF0077FF)
            AuraCyan = Color(0xFFF5A623)
        }
        "high-contrast" -> {
            // 0% OLED black + stark white, zero-glare, bold outlines.
            DarkBg = Color(0xFF000000)
            DarkSurface = Color(0xFF000000)
            DarkSurfaceGlass = Color(0xFF000000)
            DarkSurfaceVariant = Color(0xFF000000)
            DarkBorder = Color(0xFFFFFFFF)
            TextPrimary = Color(0xFFFFFFFF)
            TextSecondary = Color(0xFFFFFFFF)
            TextMuted = Color(0xFFE5E5E5)
            AuraPink = Color(0xFFFFFFFF)
            AuraCyan = Color(0xFFFFFFFF)
        }
        else -> {
            // "standard" — no override, leave the current theme colors as-is.
        }
    }
}