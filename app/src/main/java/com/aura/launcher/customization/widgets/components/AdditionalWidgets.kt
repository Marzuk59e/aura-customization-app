package com.aura.launcher.customization.widgets.components

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.core.theme.*
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClockWidget(
    accentColor: Color = AuraPurpleLight,
    modifier: Modifier = Modifier
) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            calendar = Calendar.getInstance()
            delay(1000)
        }
    }

    Surface(
        color = DarkSurfaceGlass,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Canvas Clock Face
            Canvas(modifier = Modifier.size(100.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Dial Background
                drawCircle(
                    color = Color(0xFF1E1E2C),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = DarkBorder,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                val hours = calendar.get(Calendar.HOUR)
                val minutes = calendar.get(Calendar.MINUTE)
                val seconds = calendar.get(Calendar.SECOND)

                // Hour Hand
                val hourAngle = Math.toRadians(((hours + minutes / 60.0) * 30.0) - 90.0)
                val hourEnd = Offset(
                    (center.x + radius * 0.5 * cos(hourAngle)).toFloat(),
                    (center.y + radius * 0.5 * sin(hourAngle)).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = center,
                    end = hourEnd,
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Minute Hand
                val minAngle = Math.toRadians((minutes * 6.0) - 90.0)
                val minEnd = Offset(
                    (center.x + radius * 0.75 * cos(minAngle)).toFloat(),
                    (center.y + radius * 0.75 * sin(minAngle)).toFloat()
                )
                drawLine(
                    color = accentColor,
                    start = center,
                    end = minEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Second Hand
                val secAngle = Math.toRadians((seconds * 6.0) - 90.0)
                val secEnd = Offset(
                    (center.x + radius * 0.85 * cos(secAngle)).toFloat(),
                    (center.y + radius * 0.85 * sin(secAngle)).toFloat()
                )
                drawLine(
                    color = AuraCyan,
                    start = center,
                    end = secEnd,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center Pin
                drawCircle(color = AuraCyan, radius = 4.dp.toPx(), center = center)
            }

            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = "AURA CHRONO",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Analog Vibe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Quartz Precision",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun BatteryGaugeWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var batteryPct by remember { mutableStateOf(85) }
    var isCharging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        if (level >= 0 && scale > 0) {
            batteryPct = (level * 100) / scale
        }
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    Surface(
        color = DarkSurfaceGlass,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCharging) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = if (isCharging) "Fast Charging" else "Battery Gauge",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCharging) AuraCyan else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$batteryPct%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            // Circular Meter
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { batteryPct / 100f },
                    modifier = Modifier.size(60.dp),
                    color = if (batteryPct > 20) AuraCyan else AuraPink,
                    trackColor = DarkSurfaceVariant,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (isCharging) AuraCyan else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun WeatherCardWidget(
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceGlass,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "CURRENT ATMOSPHERE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraAmber,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "28°C",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Sunny & Clear • Aura Local",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AuraAmber, AuraPink))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
