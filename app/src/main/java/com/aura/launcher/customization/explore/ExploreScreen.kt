package com.aura.launcher.customization.explore

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.aura.launcher.core.audio.SoundEngine
import com.aura.launcher.core.theme.*
import com.aura.launcher.launcher.home.HomeViewModel
import com.aura.launcher.launcher.settings.SettingsViewModel
import com.aura.launcher.customization.vibesync.ExtractedVibePalette
import com.aura.launcher.customization.icons.IconStylePack
import android.graphics.Color as AndroidColor

private enum class StudioTab { EXPLORE, VIBE_SYNC, SURPRISE, AI_STYLIST, HAPTICS, AUDIT }

private data class VibePresetInfo(
    val primary: String,
    val secondary: String,
    val tertiary: String,
    val dark: String,
    val wallpaperUrl: String
)

private val stylePresetMap = mapOf(
    "Cyberpunk" to VibePresetInfo(
        "#FF0055", "#00E5FF", "#A855F7", "#0D091A",
        "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1080&auto=format&fit=crop"
    ),
    "Minimal Bauhaus" to VibePresetInfo(
        "#E2E8F0", "#94A3B8", "#64748B", "#000000",
        "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=1080&auto=format&fit=crop"
    ),
    "True AMOLED" to VibePresetInfo(
        "#E2E8F0", "#94A3B8", "#64748B", "#000000",
        "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=1080&auto=format&fit=crop"
    ),
    "Sunset Wave" to VibePresetInfo(
        "#FF8C42", "#F7D070", "#E65100", "#120A21",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1080&auto=format&fit=crop"
    ),
    // Matches web applyPreset('emerald'/'nature') in js/studio/explore-engine.js
    "Emerald Nature" to VibePresetInfo(
        "#10B981", "#34D399", "#064E3B", "#022C22",
        "https://images.unsplash.com/photo-1518495973542-4542c06a5843?q=80&w=1080&auto=format&fit=crop"
    )
)

private val moodToPreset = mapOf(
    "Focus Mode" to ("FOCUS" to "Minimal Bauhaus"),
    "Weekend Chill" to ("CHILL" to "Sunset Wave"),
    "Night Gaming" to ("GAMING" to "Cyberpunk")
)

private fun applyVibePreset(
    presetName: String,
    homeViewModel: HomeViewModel,
    exploreViewModel: ExploreViewModel,
    settingsViewModel: SettingsViewModel,
    context: android.content.Context,
    wallpaperOverride: String? = null
) {
    val info = stylePresetMap[presetName] ?: return
    val palette = ExtractedVibePalette(
        dominant = Color(AndroidColor.parseColor(info.primary)),
        vibrant = Color(AndroidColor.parseColor(info.secondary)),
        lightVibrant = Color(AndroidColor.parseColor(info.tertiary)),
        darkVibrant = Color(AndroidColor.parseColor(info.dark)),
        muted = TextSecondary
    )
    homeViewModel.applyVibePalette(palette)
    applyThemePalette(primary = palette.dominant, secondary = palette.vibrant)

    // Persist to the same theme datastore that drives AuraLauncherTheme's
    // MaterialTheme colorScheme — this is what makes the preset change
    // cascade across every screen (buttons, chips, nav bar), not just the
    // home-screen widgets. Previously presets never reached this store.
    settingsViewModel.saveSelectedTheme(presetName, info.primary, info.secondary)

    exploreViewModel.applyWallpaperUrl(wallpaperOverride ?: info.wallpaperUrl) { }

    // Matches AudioEngine.playHapticSound('cyber') on the web preset click.
    SoundEngine.playHapticSound("cyber")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val wallpapers by viewModel.wallpapers.collectAsState()
    val iconPacks by viewModel.iconPacks.collectAsState()
    val themes by viewModel.themes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var activeCategory by remember { mutableStateOf<ExploreCategory?>(null) }
    var activePreset by remember { mutableStateOf(stylePresets.first()) }
    var activeMood by remember { mutableStateOf(moodProfiles.first()) }
    var studioTab by remember { mutableStateOf(StudioTab.EXPLORE) }
    var modeTag by remember { mutableStateOf("NORMAL") }
    var showPerfAudit by remember { mutableStateOf(false) }
    var showA11yModal by remember { mutableStateOf(false) }
    var showQuizModal by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(SoundEngine.isMuted) }
    

    Scaffold(containerColor = DarkBg) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Mobile top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceGlass)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Mode: $modeTag", color = AuraCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Explore Ground", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, DarkBorder), CircleShape)
                            .clickable { showPerfAudit = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Contrast, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, DarkBorder), CircleShape)
                            .clickable { showA11yModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color(0x1FFF4757) else DarkSurface)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, if (isMuted) Color(0x66FF4757) else DarkBorder), CircleShape)
                            .clickable {
                                isMuted = SoundEngine.toggleMuted()
                                if (!isMuted) SoundEngine.playHapticSound("crystal")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isMuted) Color(0xFFFF4757) else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Pane content
            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = studioTab, label = "studioTab") { tab ->
                    when (tab) {
                        StudioTab.EXPLORE -> LazyColumnContent(
                            isLoading = isLoading,
                            activeCategory = activeCategory,
                            onCategoryClick = { cat ->
                                activeCategory = if (activeCategory == cat) null else cat
                                cat.tab?.let { viewModel.selectTab(it) }
                                SoundEngine.playHapticSound("crystal")
                            },
                            activePreset = activePreset,
                            onPresetClick = { preset ->
                                activePreset = preset
                                applyVibePreset(preset, homeViewModel, viewModel, settingsViewModel, context)
                            },
                            activeMood = activeMood,
                            onMoodClick = { mood ->
                                activeMood = mood
                                moodToPreset[mood]?.let { (tag, preset) ->
                                    modeTag = tag
                                    applyVibePreset(preset, homeViewModel, viewModel, settingsViewModel, context)
                                }
                            },
                            selectedTab = selectedTab,
                            wallpapers = wallpapers,
                            iconPacks = iconPacks,
                            themes = themes,
                            onApplyWallpaper = { wp ->
                                viewModel.applyWallpaper(wp) { }
                            },
                            onImportWallpaper = { wp ->
                                viewModel.applyWallpaper(wp) { }
                            },
                            onNavigateToVibeSync = { studioTab = StudioTab.VIBE_SYNC },
                            onApplyIconPack = { ip ->
                                val mappedPack = when {
                                    IconStylePack.values().any { it.title.equals(ip.name, ignoreCase = true) } -> IconStylePack.values().first { it.title.equals(ip.name, ignoreCase = true) }
                                    IconStylePack.values().any { ip.name.contains(it.title, ignoreCase = true) } -> IconStylePack.values().first { ip.name.contains(it.title, ignoreCase = true) }
                                    else -> IconStylePack.DEFAULT
                                }
                                val usedFallback = mappedPack.title != ip.name && mappedPack == IconStylePack.DEFAULT
                                homeViewModel.setIconPack(mappedPack)
                                settingsViewModel.saveSelectedIconPack(ip.name)
                                if (usedFallback) {
                                    Toast.makeText(context, "This icon pack isn't fully supported yet, applying closest match.", Toast.LENGTH_LONG).show()
                                }
                            },
                            onApplyTheme = { th ->
                                val primary = try { Color(AndroidColor.parseColor(th.primaryColor)) } catch (e: Exception) { AuraPurple }
                                val secondary = try { Color(AndroidColor.parseColor(th.secondaryColor)) } catch (e: Exception) { AuraCyan }
                                val palette = ExtractedVibePalette(dominant = primary, vibrant = secondary, lightVibrant = primary, darkVibrant = Color(AndroidColor.parseColor("#14141C")), muted = TextSecondary)
                                homeViewModel.applyVibePalette(palette)
                                settingsViewModel.saveSelectedTheme(th.id, th.primaryColor, th.secondaryColor)
                                SoundEngine.playHapticSound("cyber")
                            },
                            onStyleQuizClick = {
                                showQuizModal = true
                            },
                            onSurpriseMeClick = {
                                studioTab = StudioTab.SURPRISE
                                SoundEngine.playHapticSound("velvet")
                            },
                            onHapticsClick = {
                                studioTab = StudioTab.HAPTICS
                                SoundEngine.playHapticSound("velvet")
                            },
                            onAmoledAuditClick = {
                                studioTab = StudioTab.AUDIT
                                SoundEngine.playHapticSound("velvet")
                            }
                        )
                       StudioTab.VIBE_SYNC -> VibeSyncContent(viewModel, homeViewModel, settingsViewModel)
                        StudioTab.SURPRISE -> SurpriseContent(
                            onApplyPreset = { name, wallpaperUrl ->
                                applyVibePreset(name, homeViewModel, viewModel, settingsViewModel, context, wallpaperUrl)
                            }
                        )
                        StudioTab.AI_STYLIST -> AIStylistContent(
                            onApplyPreset = { name, wallpaperUrl ->
                                applyVibePreset(name, homeViewModel, viewModel, settingsViewModel, context, wallpaperUrl)
                            }
                        )
                        StudioTab.HAPTICS -> HapticsLabContent()
                        StudioTab.AUDIT -> DeviceAuditContent()
                    }
                }
            }

            // Bottom nav
            StudioBottomNav(
                selectedTab = studioTab,
                onSelectTab = { studioTab = it },
                onHomeClick = onBack
            )
        }
    }

    if (showPerfAudit) {
        PerfAuditDialog(onOptimize = { showPerfAudit = false }, onDismiss = { showPerfAudit = false })
    }
    if (showA11yModal) {
        AccessibilityDialog(onDismiss = { showA11yModal = false })
    }
    if (showQuizModal) {
        StyleQuizDialog(
            onDismiss = { showQuizModal = false },
            onComplete = { answers ->
                showQuizModal = false
                val presetName = if (answers["step_0"] == "minimal" || answers["step_1"] == "amoled") {
                    "Minimal Bauhaus"
                } else {
                    "Cyberpunk"
                }
                applyVibePreset(presetName, homeViewModel, viewModel, settingsViewModel, context)
                onBack()
            }
        )
    }
}

@Composable
private fun StudioBottomNav(
    selectedTab: StudioTab,
    onSelectTab: (StudioTab) -> Unit,
    onHomeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceGlass)
            .border(androidx.compose.foundation.BorderStroke(1.dp, DarkBorder))
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavTabItem(Icons.Default.Explore, "Explore", selectedTab == StudioTab.EXPLORE) { onSelectTab(StudioTab.EXPLORE) }
        NavTabItem(Icons.Default.CameraAlt, "Vibe Sync", selectedTab == StudioTab.VIBE_SYNC) { onSelectTab(StudioTab.VIBE_SYNC) }

        // Center FAB
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onHomeClick)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(AuraPink, AuraCyan))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        NavTabItem(Icons.Default.Shuffle, "Surprise", selectedTab == StudioTab.SURPRISE) { onSelectTab(StudioTab.SURPRISE) }
        NavTabItem(Icons.Default.AutoAwesome, "AI Stylist", selectedTab == StudioTab.AI_STYLIST) { onSelectTab(StudioTab.AI_STYLIST) }
    }
}

@Composable
private fun NavTabItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (active) AuraPink else TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (active) AuraPink else TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessibilityDialog(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf("standard") }

    fun safeDismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(Color(0xFF00F0FF).copy(alpha = 0.4f))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                )
                Spacer(Modifier.height(14.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Accessibility & Visual Presets", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text("Ensure maximum contrast, color-blind harmony, and readability.", color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    A11yOptionCard(
                        selected = selected == "standard", onClick = { selected = "standard" },
                        badgeColor = AuraPink, badgeBg = AuraPink.copy(alpha = 0.15f), badgeBorder = AuraPink.copy(alpha = 0.3f),
                        badge = "STANDARD", title = "Standard High Dynamic",
                        mainPoint = "✦ Full Dynamic Material You Spectrum",
                        desc = "For: General everyday users wanting rich colorful gradients, fluid transparencies, and default vibrant launcher aesthetics."
                    )
                    A11yOptionCard(
                        selected = selected == "deuteranopia", onClick = { selected = "deuteranopia" },
                        badgeColor = Color(0xFF38BDF8), badgeBg = Color(0xFF0077FF).copy(alpha = 0.18f), badgeBorder = Color(0xFF0077FF).copy(alpha = 0.4f),
                        badge = "COLOR BLIND HARMONY", title = "Deuteranopia & Protanopia",
                        mainPoint = "👁 High-Contrast Blue & Amber Palette",
                        desc = "For: Users with Red-Green color deficiency. Eliminates confusing red/green mixes by shifting all UI accents into crisp, distinguishable cobalt blue and warm gold."
                    )
                    A11yOptionCard(
                        selected = selected == "high-contrast", onClick = { selected = "high-contrast" },
                        badgeColor = Color.White, badgeBg = Color.White.copy(alpha = 0.15f), badgeBorder = Color.White.copy(alpha = 0.35f),
                        badge = "HIGH VISIBILITY", title = "Extreme High-Contrast Monochrome",
                        mainPoint = "🌙 0% OLED Black & Crisp Stark White",
                        desc = "For: Users with low vision, visual fatigue, or direct outdoor sunlight usage. Bold outlines, zero glare."
                    )
                }
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .clickable { safeDismiss() }
                        .padding(11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Close", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PerfMetricRow(label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// Mirrors the web #modal-perf-audit sheet (css/views/explore-ground.css +
// css/components/modals.css): same tag/title/subtitle copy, same 3 metric
// rows, same gradient primary button and plain close button.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerfAuditDialog(onOptimize: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    fun safeDismiss(onComplete: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onComplete()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = AuraPink.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraPink.copy(alpha = 0.25f))
                ) {
                    Text(
                        "⚡ PERFORMANCE ENGINE",
                        color = AuraPink,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Adaptive Device Optimization", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Row {
                    Text("Current Profile: ", color = TextSecondary, fontSize = 11.sp)
                    Text("Smooth / High Capability", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PerfMetricRow("Active Home Screen Widgets", "3 Hosted Widgets (Light)", TextPrimary)
                PerfMetricRow("Backdrop Blur Rendering", "Hardware Accelerated (GPU)", TextPrimary)
                PerfMetricRow("OLED Black Pixel Coverage", "94% (High Savings)", AuraSuccess)
            }
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(AuraPink, Color(0xFF990033))))
                    .clickable(onClick = { safeDismiss(onOptimize) })
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Optimize Without Changing My Style", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .clickable { safeDismiss(onDismiss) }
                    .padding(11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Close", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun A11yOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    badgeColor: Color,
    badgeBg: Color,
    badgeBorder: Color,
    badge: String,
    title: String,
    mainPoint: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) AuraCyan.copy(alpha = 0.08f) else DarkSurfaceVariant)
            .border(1.5.dp, if (selected) AuraCyan else DarkBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorder)
            ) {
                Text(
                    badge,
                    color = badgeColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(mainPoint, color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(desc, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
    }
}


private data class QuizChoice(val label: String, val desc: String, val icon: String, val value: String)
private data class QuizQuestion(val title: String, val choices: List<QuizChoice>)

private val quizQuestions = listOf(
    QuizQuestion(
        "Which visual aesthetic resonates with you?",
        listOf(
            QuizChoice("Minimal Bauhaus", "Clean typography & monochrome slate", "🏛️", "minimal"),
            QuizChoice("Cyberpunk Neon", "Glowing HUD & futuristic purple/cyan", "⚡", "cyber")
        )
    ),
    QuizQuestion(
        "Choose your lighting environment:",
        listOf(
            QuizChoice("AMOLED Pure Black", "Maximum battery saver (0% OLED)", "🌙", "amoled"),
            QuizChoice("Warm Pastel Sunset", "Cozy golden dusk tones", "🌅", "sunset")
        )
    ),
    QuizQuestion(
        "What tactile feedback sound do you prefer?",
        listOf(
            QuizChoice("Crystal Chimes", "High-frequency airy chimes", "💎", "crystal"),
            QuizChoice("Mechanical Thock", "Subtle lubed switch click", "⌨️", "mechanical")
        )
    ),
    QuizQuestion(
        "What should take center stage on your home screen?",
        listOf(
            QuizChoice("Precision Clock Widget", "Bold typography & time focus", "⏱️", "clock"),
            QuizChoice("Memory Photo Frame", "Polaroid slideshow of memories", "📸", "photo")
        )
    )
)

@Composable
private fun StyleQuizDialog(onDismiss: () -> Unit, onComplete: (Map<String, String>) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var answers by remember { mutableStateOf(mapOf<String, String>()) }
    val question = quizQuestions[step]

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Column {
                LinearProgressIndicator(
                    progress = { (step + 1) / quizQuestions.size.toFloat() },
                    color = AuraPink,
                    trackColor = DarkBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(12.dp))
                Text("Question ${step + 1} of ${quizQuestions.size}", color = AuraCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(question.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                question.choices.forEach { choice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                val stepKey = "step_$step"
                                val updatedAnswers = answers + (stepKey to choice.value)
                                answers = updatedAnswers
                                SoundEngine.playHapticSound("crystal")
                                if (step < quizQuestions.size - 1) {
                                    step += 1
                                } else {
                                    onComplete(updatedAnswers)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(choice.icon, fontSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(choice.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(choice.desc, color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Skip Quiz", color = TextSecondary) }
        }
    )
}