package com.aura.launcher.customization.widgets

import android.content.ComponentName

enum class WidgetType {
    DIGITAL_CLOCK_NEON,
    DIGITAL_CLOCK_MINIMAL,
    ANALOG_CLOCK,
    BATTERY_GAUGE,
    WEATHER_CARD,
    SYSTEM_WIDGET   // Real Android AppWidget (third-party or system) — not one of our built-in widgets
}

data class AuraWidgetInfo(
    val type: WidgetType,
    val title: String,
    val description: String,
    val spanX: Int = 4,
    val spanY: Int = 2,
    val category: String = "Essential",
    val componentName: ComponentName? = null   // only set when type == SYSTEM_WIDGET
)