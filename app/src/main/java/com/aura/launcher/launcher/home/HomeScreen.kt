package com.aura.launcher.launcher.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.core.theme.*
import com.aura.launcher.customization.icons.IconPackPickerSheet
import com.aura.launcher.customization.vibesync.VibeSyncDialog
import com.aura.launcher.customization.widgets.WidgetPickerSheet
import com.aura.launcher.customization.widgets.WidgetType
import com.aura.launcher.customization.widgets.components.AnalogClockWidget
import com.aura.launcher.customization.widgets.components.BatteryGaugeWidget
import com.aura.launcher.customization.widgets.components.DigitalClockNeonWidget
import com.aura.launcher.customization.widgets.components.WeatherCardWidget
import com.aura.launcher.domain.model.HomeItem
import com.aura.launcher.domain.model.HomeItemType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onOpenAppDrawer: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenSavedSetups: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val homeItems by homeViewModel.page0Items.collectAsState()
    val dockItems by homeViewModel.dockItems.collectAsState()
    val iconShape by homeViewModel.activeIconShape.collectAsState()
    val iconPack by homeViewModel.activeIconPack.collectAsState()
    val vibePalette by homeViewModel.vibePalette.collectAsState()

    var showWidgetPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showVibeSyncDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -30) {
                        onOpenAppDrawer()
                    }
                }
            }
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, bottom = 24.dp)
        ) {
            // Top HUD: matches web design's launcher-top-hud (role badge + Studio button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Role badge: "Aura Default Launcher" with pulsing live dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DarkSurfaceGlass)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AuraSuccess.copy(alpha = dotAlpha))
                    )
                    Text(
                        "Aura Default Launcher",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Settings (kept accessible, not in original web mock but needed for app functionality)
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.clip(CircleShape).background(DarkSurfaceGlass).size(34.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Settings", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }

                    // Studio button -> opens Explore Ground (customization studio)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(AuraPink, Color(0xFF990033))
                                )
                            )
                            .clickable(onClick = onOpenExplore)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Studio", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Dynamic Home Grid (Widgets & Apps)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Default Top Clock Widget
                item(span = { GridItemSpan(4) }) {
                    DigitalClockNeonWidget(accentColor = vibePalette.vibrant)
                }

                // Render dynamic Home items
                items(homeItems, key = { it.id }, span = { item ->
                    if (item.itemType == HomeItemType.WIDGET) GridItemSpan(item.spanX) else GridItemSpan(1)
                }) { item ->
                    if (item.itemType == HomeItemType.WIDGET) {
                        when (item.widgetProvider) {
                            WidgetType.ANALOG_CLOCK.name -> AnalogClockWidget(accentColor = vibePalette.vibrant)
                            WidgetType.BATTERY_GAUGE.name -> BatteryGaugeWidget()
                            WidgetType.WEATHER_CARD.name -> WeatherCardWidget()
                            else -> DigitalClockNeonWidget(accentColor = vibePalette.vibrant)
                        }
                    } else {
                        HomeGridItem(
                            item = item,
                            iconShape = iconShape,
                            accentColor = vibePalette.lightVibrant,
                            onClick = { homeViewModel.launchHomeItem(item) }
                        )
                    }
                }
            }

            // Bottom Drawer Handle / Search Bar Prompt
            Surface(
                color = DarkSurfaceGlass,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenAppDrawer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = vibePalette.vibrant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Swipe up or tap for apps...", color = TextSecondary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dock
            HomeDock(
                dockItems = dockItems,
                iconShape = iconShape,
                accentColor = vibePalette.vibrant,
                onItemClick = { homeViewModel.launchHomeItem(it) },
                onOpenAppDrawer = onOpenAppDrawer
            )
        }

        // Modals & Bottom Sheets
        if (showWidgetPicker) {
            WidgetPickerSheet(
                onDismiss = { showWidgetPicker = false },
                onSelectWidget = { homeViewModel.addWidgetToHome(it) }
            )
        }

        if (showIconPicker) {
            IconPackPickerSheet(
                activePack = iconPack,
                activeShape = iconShape,
                onDismiss = { showIconPicker = false },
                onSelectPack = { homeViewModel.setIconPack(it) },
                onSelectShape = { homeViewModel.setIconShape(it) }
            )
        }

        if (showVibeSyncDialog) {
            VibeSyncDialog(
                currentPalette = vibePalette,
                onDismiss = { showVibeSyncDialog = false },
                onApplyPalette = { homeViewModel.applyVibePalette(it) }
            )
        }
    }
}