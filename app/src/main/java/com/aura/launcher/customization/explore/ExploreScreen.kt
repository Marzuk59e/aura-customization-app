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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.core.audio.SoundEngine
import com.aura.launcher.core.theme.*
import com.aura.launcher.launcher.home.HomeViewModel
import com.aura.launcher.launcher.settings.SettingsViewModel
import com.aura.launcher.customization.vibesync.ExtractedVibePalette
import com.aura.launcher.customization.icons.IconStylePack
import android.graphics.Color as AndroidColor

private enum class StudioTab { EXPLORE, VIBE_SYNC, SURPRISE, AI_STYLIST }

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
    context: android.content.Context
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

    // Persist to the same theme datastore that drives AuraLauncherTheme's
    // MaterialTheme colorScheme — this is what makes the preset change
    // cascade across every screen (buttons, chips, nav bar), not just the
    // home-screen widgets. Previously presets never reached this store.
    settingsViewModel.saveSelectedTheme(presetName, info.primary, info.secondary)

    exploreViewModel.applyWallpaperUrl(info.wallpaperUrl) { success ->
        Toast.makeText(
            context,
            if (success) "Preset applied: $presetName" else "Colors applied, wallpaper failed",
            Toast.LENGTH_SHORT
        ).show()
    }

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
    var isMuted by remember { mutableStateOf(false) }

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
                    listOf(Icons.Default.Contrast, Icons.Default.Visibility).forEach { icon ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .clickable {
                                isMuted = SoundEngine.toggleMuted()
                                if (!isMuted) SoundEngine.playHapticSound("crystal")
                                Toast.makeText(
                                    context,
                                    if (isMuted) "Sound & Haptics: MUTED" else "Sound & Haptics: ON",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = TextSecondary,
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
                                viewModel.applyWallpaper(wp) { success ->
                                    Toast.makeText(context, if (success) "Wallpaper Applied!" else "Failed to apply wallpaper", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onImportWallpaper = { wp ->
                                viewModel.applyWallpaper(wp) { success ->
                                    Toast.makeText(context, if (success) "Setup Imported!" else "Failed to import setup", Toast.LENGTH_SHORT).show()
                                }
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
                                Toast.makeText(context, if (usedFallback) "This icon pack isn't fully supported yet, applying closest match." else "Icon pack applied: ${mappedPack.title}", Toast.LENGTH_LONG).show()
                            },
                            onApplyTheme = { th ->
                                val primary = try { Color(AndroidColor.parseColor(th.primaryColor)) } catch (e: Exception) { AuraPurple }
                                val secondary = try { Color(AndroidColor.parseColor(th.secondaryColor)) } catch (e: Exception) { AuraCyan }
                                val palette = ExtractedVibePalette(dominant = primary, vibrant = secondary, lightVibrant = primary, darkVibrant = Color(AndroidColor.parseColor("#14141C")), muted = TextSecondary)
                                homeViewModel.applyVibePalette(palette)
                                settingsViewModel.saveSelectedTheme(th.id, th.primaryColor, th.secondaryColor)
                                SoundEngine.playHapticSound("cyber")
                                Toast.makeText(context, "Theme applied: ${th.name}", Toast.LENGTH_SHORT).show()
                            }
                        )
                        StudioTab.VIBE_SYNC -> VibeSyncContent(viewModel)
                        StudioTab.SURPRISE -> SurpriseContent()
                        StudioTab.AI_STYLIST -> AIStylistContent()
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