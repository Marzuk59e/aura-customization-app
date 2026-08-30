package com.aura.launcher.customization.explore

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aura.launcher.core.theme.*
import com.aura.launcher.domain.model.*

@Composable
fun LazyColumnContent(
    isLoading: Boolean,
    activeCategory: ExploreCategory?,
    onCategoryClick: (ExploreCategory) -> Unit,
    activePreset: String,
    onPresetClick: (String) -> Unit,
    activeMood: String,
    onMoodClick: (String) -> Unit,
    selectedTab: ExploreTab,
    wallpapers: List<RemoteWallpaper>,
    iconPacks: List<RemoteIconPack>,
    themes: List<RemoteTheme>,
    onApplyWallpaper: (RemoteWallpaper) -> Unit,
    onImportWallpaper: (RemoteWallpaper) -> Unit,
    onNavigateToVibeSync: () -> Unit,
    onApplyIconPack: (RemoteIconPack) -> Unit,
    onApplyTheme: (RemoteTheme) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFF0055).copy(alpha = 0.15f), Color(0xFF00E5FF).copy(alpha = 0.08f)))
                    )
                    .background(DarkSurface.copy(alpha = 0.4f))
                    v
                    .padding(16.dp)
            ) {
                Column {
                    Surface(
                        color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                    ) {
                        Text(
                            "★ FLAGSHIP #2",
                            color = AuraCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("One-Tap \"Vibe Sync\"", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Snap or pick a photo. Match your clock, wallpaper & widgets instantly.",
                        color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(Color(0xFFFF0055), Color(0xFF00E5FF), Color(0xFFA855F7), Color(0xFF1E153A)).forEach { c ->
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(c))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                   Button(
                        onClick = onNavigateToVibeSync,
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPink),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sync Photo", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Customization Categories", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Explore Ground", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
        }
        item {
            CategoryGrid(exploreCategories, activeCategory, onCategoryClick)
            Spacer(Modifier.height(20.dp))
        }

        item {
            Text("Style Presets", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stylePresets.forEach { preset ->
                    val active = preset == activePreset
                    Surface(
                        color = if (active) AuraCyan else DarkSurface,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) AuraCyan else DarkBorder),
                        modifier = Modifier.clickable { onPresetClick(preset) }
                    ) {
                        Text(
                            preset,
                            color = if (active) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("⚡ Launcher Mood Profiles", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Contextual Switch", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodProfiles.forEach { mood ->
                    val active = mood == activeMood
                    Surface(
                        color = if (active) AuraCyan.copy(alpha = 0.12f) else DarkSurface,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) AuraCyan else DarkBorder),
                        modifier = Modifier.clickable { onMoodClick(mood) }
                    ) {
                        Text(
                            mood,
                            color = if (active) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            QuickActionRow()
            Spacer(Modifier.height(24.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🔥 Trending Launcher Setups", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Top Community Codes", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                wallpapers.take(4).forEachIndexed { index, wp ->
                    TrendingCard(rank = index + 1, wallpaper = wp, onImport = { onImportWallpaper(wp) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (activeCategory?.tab != null) {
            item {
                if (isLoading) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AuraPurple)
                    }
                }
            }
            when (selectedTab) {
                ExploreTab.WALLPAPERS -> items(wallpapers, key = { "wp-${it.id}" }) { wp ->
                    WallpaperCard(wallpaper = wp, onApply = { onApplyWallpaper(wp) })
                    Spacer(Modifier.height(14.dp))
                }
                ExploreTab.ICON_PACKS -> items(iconPacks, key = { "ip-${it.id}" }) { ip ->
                    IconPackCard(iconPack = ip, onApply = { onApplyIconPack(ip) })
                    Spacer(Modifier.height(14.dp))
                }
                ExploreTab.THEMES -> items(themes, key = { "th-${it.id}" }) { th ->
                    ThemeCard(theme = th, onApply = { onApplyTheme(th) })
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

private data class VibeOption(val label: String, val badge: String, val imageUrl: String, val colors: List<Color>)

private val vibeOptions = listOf(
    VibeOption("Tokyo Cyberpunk", "CYBER NEON", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=400&auto=format&fit=crop",
        listOf(Color(0xFFFF0055), Color(0xFF00E5FF), Color(0xFFA855F7), Color(0xFF0D091A))),
    VibeOption("Golden Coast", "WARM DUSK", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=400&auto=format&fit=crop",
        listOf(Color(0xFFFF8C42), Color(0xFFF7D070), Color(0xFFE65100), Color(0xFF120A21))),
    VibeOption("Monochrome Slate", "MINIMALIST", "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=400&auto=format&fit=crop",
        listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFF090D16))),
    VibeOption("Misty Emerald Forest", "NATURE", "https://images.unsplash.com/photo-1518495973542-4542c06a5843?q=80&w=400&auto=format&fit=crop",
        listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF064E3B), Color(0xFF022C22))),
)

@Composable
fun VibeSyncContent(viewModel: ExploreViewModel) {
    var selected by remember { mutableStateOf(vibeOptions.first()) }
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
    ) {
        item {
            Surface(color = Color(0xFFFF0055).copy(alpha = 0.12f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF0055).copy(alpha = 0.3f))) {
                Text("★ FLAGSHIP IDEA #2", color = AuraPink, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("One-Tap \"Vibe Sync\"", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Extract instant Material You palettes from photos and synchronize your wallpaper, clock typography & live widgets.",
                color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            vibeOptions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { vibe ->
                        val active = vibe == selected
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                                .border(1.dp, if (active) AuraPink else DarkBorder, RoundedCornerShape(16.dp))
                                .clickable { selected = vibe }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                                AsyncImage(model = vibe.imageUrl, contentDescription = vibe.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                ) {
                                    Text(vibe.badge, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Column(Modifier.padding(10.dp)) {
                                Text(vibe.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    vibe.colors.forEach { c -> Box(Modifier.size(10.dp).clip(CircleShape).background(c)) }
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(6.dp))
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkSurface.copy(alpha = 0.4f))
                    .border(1.dp, AuraCyan.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .clickable { 
                        // Mock photo processing
                        Toast.makeText(context, "Scanning photo for vibe...", Toast.LENGTH_SHORT).show()
                    }
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(AuraCyan.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text("Upload or Snap Any Photo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Instant on-device quantization in <1.2s", color = TextMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Extracted Material You Palette", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("5 Dynamic Tokens", color = AuraCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (selected.colors + Color(0xFF0D091A)).forEach { c ->
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c).border(1.dp, DarkBorder, RoundedCornerShape(10.dp)))
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            Button(
                onClick = { 
                    viewModel.applyVibeSync { success ->
                        Toast.makeText(context, if (success) "Vibe Applied!" else "Failed to apply vibe", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraPink),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sync Entire Phone & Launcher To This Vibe", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

private data class TimePreset(val time: String, val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

private val timePresets = listOf(
    TimePreset("07:00 AM", "Sunrise Fresh", "Energizing emerald hues & morning weather", Icons.Default.WbSunny, Color(0xFF10B981)),
    TimePreset("11:30 AM", "Workday Focus", "Monochrome minimal typography & agenda", Icons.Default.Work, Color(0xFFE2E8F0)),
    TimePreset("06:45 PM", "Golden Dusk", "Warm amber glow & memory frame", Icons.Default.WbTwilight, Color(0xFFFF8C42)),
    TimePreset("11:15 PM", "Midnight AMOLED", "Pure 0% black OLED with neon cyber accent", Icons.Default.NightsStay, Color(0xFFA855F7)),
)

@Composable
fun SurpriseContent() {
    var selected by remember { mutableStateOf(timePresets.last()) }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Surface(color = Color(0xFFFF0055).copy(alpha = 0.12f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF0055).copy(alpha = 0.3f))) {
            Text("★ FLAGSHIP IDEA #6", color = AuraPink, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("Surprise Me (Contextual Shuffle)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Smart rotation matching your home screen vibe according to the time of day or single-tap surprise.", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            timePresets.forEach { preset ->
                val active = preset == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) AuraCyan.copy(alpha = 0.08f) else DarkSurface)
                        .border(1.dp, if (active) AuraCyan else DarkBorder, RoundedCornerShape(14.dp))
                        .clickable { selected = preset }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(preset.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(preset.icon, contentDescription = null, tint = preset.color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("${preset.time} • ${preset.title}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(preset.subtitle, color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = { selected = timePresets.random() },
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Roll Random Surprise Inspiration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

private data class ChatMessage(val fromUser: Boolean, val text: String)

private val aiSuggestionChips = listOf(
    "🌧️ Cozy Anime Rain" to "Aesthetic cozy anime rainy night with soft neon widgets",
    "⚡ AMOLED Extreme Battery" to "Ultra-minimal AMOLED battery saving setup for focus",
    "👑 Luxury Gold" to "Luxury gold and dark emerald executive business look"
)

@Composable
fun AIStylistContent() {
    var messages by remember {
        mutableStateOf(listOf(ChatMessage(false, "Hello Alex! I am your Aura AI Stylist. Tell me what vibe or layout you want on your Home Screen!")))
    }
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Surface(color = Color(0xFFFF0055).copy(alpha = 0.12f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF0055).copy(alpha = 0.3f))) {
            Text("🤖 HYBRID AI ENGINE", color = AuraPink, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("AI Launcher Stylist", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Describe your vibe or ask for layout optimizations. Aura re-organizes your real launcher instantly.", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            messages.forEach { msg ->
                if (msg.fromUser) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(color = AuraPink, shape = RoundedCornerShape(12.dp, 12.dp, 2.dp, 12.dp), modifier = Modifier.widthIn(max = 240.dp)) {
                            Text(msg.text, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(AuraPink), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Surface(color = DarkSurfaceVariant, shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp), modifier = Modifier.widthIn(max = 240.dp)) {
                            Text(msg.text, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            aiSuggestionChips.forEach { (label, prompt) ->
                Surface(
                    color = DarkSurface, shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.clickable { input = prompt }
                ) {
                    Text(label, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.text.BasicTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AuraPink),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuraPink)
                    .clickable {
                        if (input.isNotBlank()) {
                            messages = messages + ChatMessage(true, input) + ChatMessage(false, "Got it — applying that vibe to your Home Screen now.")
                            input = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}
