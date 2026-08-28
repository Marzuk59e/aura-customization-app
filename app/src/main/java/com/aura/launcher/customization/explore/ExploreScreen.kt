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
import com.aura.launcher.core.theme.*

private enum class StudioTab { EXPLORE, VIBE_SYNC, SURPRISE, AI_STYLIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
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
                        Text("Mode: Normal", color = AuraCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Explore Ground", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Icons.Default.Contrast, Icons.Default.Visibility, Icons.Default.VolumeUp).forEach { icon ->
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
                            },
                            activePreset = activePreset,
                            onPresetClick = { activePreset = it },
                            activeMood = activeMood,
                            onMoodClick = { activeMood = it },
                            selectedTab = selectedTab,
                            wallpapers = wallpapers,
                            iconPacks = iconPacks,
                            themes = themes,
                            onApplyWallpaper = { wp ->
                                viewModel.applyWallpaper(wp) { success ->
                                    Toast.makeText(context, if (success) "Wallpaper Applied!" else "Failed to apply wallpaper", Toast.LENGTH_SHORT).show()
                                }
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
