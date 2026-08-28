package com.aura.launcher.core.theme

import androidx.compose.ui.graphics.Color

// Matches css/base/variables.css from the aura-customization-app web design
var AuraPurple = Color(0xFFA855F7)      // --accent-tertiary
var AuraPurpleLight = Color(0xFFC084FC)
var AuraCyan = Color(0xFF00E5FF)        // --accent-secondary
var AuraPink = Color(0xFFFF0055)        // --accent-primary
var AuraAmber = Color(0xFFF59E0B)       // --color-warning
var AuraSuccess = Color(0xFF10B981)     // --color-success

var DarkBg = Color(0xFF05070C)          // --bg-app
var DarkSurface = Color(0xFF10131C)     // --bg-card
var DarkSurfaceGlass = Color(0xE010131C) // --bg-glass
var DarkSurfaceVariant = Color(0xFF181D2A) // --bg-card-elevated
var DarkBorder = Color(0x14FFFFFF)      // --border-subtle

var TextPrimary = Color(0xFFF8FAFC)     // --text-main
var TextSecondary = Color(0xFF94A3B8)   // --text-sub
var TextMuted = Color(0xFF64748B)       // --text-muted

var LightBg = Color(0xFFF7F8FC)
var LightSurface = Color(0xFFFFFFFF)
var LightSurfaceGlass = Color(0xCCFFFFFF)
var LightSurfaceVariant = Color(0xFFEAEBF2)
var LightBorder = Color(0x1A000000)

fun resetDefaultPalette() {
    AuraPurple = Color(0xFFA855F7)
    AuraPurpleLight = Color(0xFFC084FC)
    AuraCyan = Color(0xFF00E5FF)
    AuraPink = Color(0xFFFF0055)
    AuraAmber = Color(0xFFF59E0B)
    AuraSuccess = Color(0xFF10B981)

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
    AuraPurple = primary
    AuraPurpleLight = primary.copy(alpha = 0.75f)
    AuraCyan = secondary
    AuraPink = primary.copy(alpha = 0.9f)

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