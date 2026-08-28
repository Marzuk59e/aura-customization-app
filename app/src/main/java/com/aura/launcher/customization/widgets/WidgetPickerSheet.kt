package com.aura.launcher.customization.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.core.theme.*

val BuiltInWidgetsList = listOf(
    AuraWidgetInfo(
        type = WidgetType.DIGITAL_CLOCK_NEON,
        title = "Neon Digital Clock",
        description = "Futuristic glowing digital clock with real-time seconds & date.",
        spanX = 4,
        spanY = 2
    ),
    AuraWidgetInfo(
        type = WidgetType.ANALOG_CLOCK,
        title = "Chrono Analog Clock",
        description = "Smooth canvas quartz clock with animated hour/minute hands.",
        spanX = 4,
        spanY = 2
    ),
    AuraWidgetInfo(
        type = WidgetType.BATTERY_GAUGE,
        title = "Neon Battery Gauge",
        description = "Real-time battery percentage with charging indicator.",
        spanX = 4,
        spanY = 2
    ),
    AuraWidgetInfo(
        type = WidgetType.WEATHER_CARD,
        title = "Aura Weather Card",
        description = "Live temperature and atmosphere conditions.",
        spanX = 4,
        spanY = 2
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
    onDismiss: () -> Unit,
    onSelectWidget: (AuraWidgetInfo) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Widget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(BuiltInWidgetsList) { widget ->
                    WidgetPickerCard(
                        widget = widget,
                        onClick = {
                            onSelectWidget(widget)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetPickerCard(
    widget: AuraWidgetInfo,
    onClick: () -> Unit
) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            when (widget.type) {
                                WidgetType.DIGITAL_CLOCK_NEON -> listOf(AuraPurple, AuraCyan)
                                WidgetType.ANALOG_CLOCK -> listOf(AuraPurple, AuraPink)
                                WidgetType.BATTERY_GAUGE -> listOf(AuraCyan, Color(0xFF00E676))
                                WidgetType.WEATHER_CARD -> listOf(AuraAmber, AuraPink)
                                else -> listOf(AuraPurple, AuraCyan)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (widget.type) {
                        WidgetType.DIGITAL_CLOCK_NEON -> Icons.Default.AccessTime
                        WidgetType.ANALOG_CLOCK -> Icons.Default.Schedule
                        WidgetType.BATTERY_GAUGE -> Icons.Default.BatteryChargingFull
                        WidgetType.WEATHER_CARD -> Icons.Default.WbSunny
                        else -> Icons.Default.Widgets
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = widget.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = widget.description, color = TextSecondary, fontSize = 12.sp)
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AuraPurple)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
