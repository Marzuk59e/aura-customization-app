package com.aura.launcher.launcher.home

import android.content.ComponentName
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.ui.draw.drawBehind

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onOpenAppDrawer: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onOpenSavedSetups: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val homeItems by homeViewModel.page0Items.collectAsState()
    val allHomeItems by homeViewModel.allHomeItems.collectAsState()
    val pageCount by homeViewModel.pageCount.collectAsState()
    val dockItems by homeViewModel.dockItems.collectAsState()
    val iconShape by homeViewModel.activeIconShape.collectAsState()
    val iconPack by homeViewModel.activeIconPack.collectAsState()
    val vibePalette by homeViewModel.vibePalette.collectAsState()
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { pageCount })

    LaunchedEffect(pagerState.currentPage) {
        homeViewModel.setActivePage(pagerState.currentPage)
    }

    var showWidgetPicker by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showVibeSyncDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Ambient gradient overlay — matches css .launcher-ambient-overlay
        // linear-gradient(180deg, rgba(0,0,0,0.3) 0%, transparent 20%, transparent 70%, rgba(0,0,0,0.65) 100%)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.3f),
                        0.2f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.65f)
                    )
                )
        )

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
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { isEditMode = true })
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
                            .drawBehind {
                                drawCircle(color = AuraSuccess.copy(alpha = dotAlpha))
                            }
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

            // Main Dynamic Home Grid — multi-page swipeable surface, matches css .launcher-pages-container
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageItems = remember(allHomeItems, page) {
                    allHomeItems.filter { it.pageIndex == page }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Default Top Clock Widget — only on the first page
                    if (page == 0) {
                        item(span = { GridItemSpan(4) }) {
                            DigitalClockNeonWidget(accentColor = vibePalette.vibrant)
                        }
                    }

                    // Render dynamic Home items for this page
                    items(pageItems, key = { it.id }, span = { item ->
                        if (item.itemType == HomeItemType.WIDGET) GridItemSpan(item.spanX) else GridItemSpan(1)
                    }) { item ->
                        if (item.itemType == HomeItemType.WIDGET) {
                            if (item.widgetId != null && item.widgetProvider != null) {
                                RealWidgetHost(
                                    appWidgetId = item.widgetId,
                                    provider = ComponentName.unflattenFromString(item.widgetProvider),
                                    appWidgetHostHelper = homeViewModel.appWidgetHostHelper
                                )
                            } else {
                                when (item.widgetProvider) {
                                    WidgetType.ANALOG_CLOCK.name -> AnalogClockWidget(accentColor = vibePalette.vibrant)
                                    WidgetType.BATTERY_GAUGE.name -> BatteryGaugeWidget()
                                    WidgetType.WEATHER_CARD.name -> WeatherCardWidget()
                                    else -> DigitalClockNeonWidget(accentColor = vibePalette.vibrant)
                                }
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
            }

            // Page Dots Indicator — matches css .launcher-page-dots / .page-dot
            if (pageCount > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pageCount) { index ->
                        val isActive = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .height(6.dp)
                                .width(if (isActive) 16.dp else 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isActive) AuraCyan else Color.White.copy(alpha = 0.3f))
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

        // Edit Mode Toolbar — matches css .launcher-edit-toolbar (long-press home surface to trigger)
        AnimatedVisibility(
            visible = isEditMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 92.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            EditModeToolbar(
                onAddWidget = { showWidgetPicker = true },
                onAddFolder = {
                    Toast.makeText(context, "Folder creation is coming soon", Toast.LENGTH_SHORT).show()
                },
                onOpenWallpaper = {
                    isEditMode = false
                    onOpenWallpaper()
                },
                onDone = { isEditMode = false }
            )
        }

        // Modals & Bottom Sheets
        if (showWidgetPicker) {
            WidgetPickerSheet(
                appWidgetHostHelper = homeViewModel.appWidgetHostHelper,
                onDismiss = { showWidgetPicker = false },
                onSelectWidget = { homeViewModel.addWidgetToHome(it, pagerState.currentPage) }
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
}

// Matches css .launcher-edit-toolbar / .edit-toolbar-title / .edit-actions-row / .edit-btn
@Composable
private fun EditModeToolbar(
    onAddWidget: () -> Unit,
    onAddFolder: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onDone: () -> Unit
) {
    Surface(
        color = DarkSurfaceGlass,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(14.dp))
                Text("Home Screen Edit Mode", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                EditToolbarButton(Icons.Default.AddCircle, "Widget", modifier = Modifier.weight(1f), onClick = onAddWidget)
                EditToolbarButton(Icons.Default.CreateNewFolder, "Folder", modifier = Modifier.weight(1f), onClick = onAddFolder)
                EditToolbarButton(Icons.Default.Image, "Wallpaper", modifier = Modifier.weight(1f), onClick = onOpenWallpaper)
                EditToolbarButton(Icons.Default.Check, "Done", modifier = Modifier.weight(1f), isDone = true, onClick = onDone)
            }
        }
    }
}

@Composable
private fun EditToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    isDone: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDone) AuraSuccess else DarkSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(15.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RealWidgetHost(
    appWidgetId: Int,
    provider: ComponentName?,
    appWidgetHostHelper: com.aura.launcher.core.utils.AppWidgetHostHelper
) {
    if (provider == null) return
    val hostView = remember(appWidgetId) {
        appWidgetHostHelper.createHostView(appWidgetId, provider)
    }
    if (hostView != null) {
        AndroidView(
            factory = { hostView },
            modifier = Modifier.fillMaxWidth()
        )
    }
}