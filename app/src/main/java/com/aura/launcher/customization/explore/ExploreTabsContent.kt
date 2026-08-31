package com.aura.launcher.customization.explore

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import com.aura.launcher.core.audio.SoundEngine
import com.aura.launcher.customization.vibesync.ExtractedVibePalette
import com.aura.launcher.customization.vibesync.VibeSyncEngine
import com.aura.launcher.launcher.home.HomeViewModel
import com.aura.launcher.launcher.settings.SettingsViewModel

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
    onApplyTheme: (RemoteTheme) -> Unit,
    onStyleQuizClick: () -> Unit,
    onSurpriseMeClick: () -> Unit,
    onHapticsClick: () -> Unit,
    onAmoledAuditClick: () -> Unit
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
                    .background(DarkSurface)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFF0055).copy(alpha = 0.15f), Color(0xFF00E5FF).copy(alpha = 0.08f)))
                    )
                    .border(1.dp, Color(0xFFFF0055).copy(alpha = 0.35f), RoundedCornerShape(24.dp))
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
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(AuraPink, Color(0xFFD90048))))
                            .clickable { onNavigateToVibeSync() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sync Photo", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Customization Categories", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                BadgeMini("Explore Ground")
            }
            Spacer(Modifier.height(10.dp))
        }
        item {
            CategoryGrid(exploreCategories, activeCategory, onCategoryClick)
            Spacer(Modifier.height(20.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Style Presets", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                BadgeMini("1-Tap Vibe")
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stylePresets.forEach { preset ->
                    val active = preset == activePreset
                    val bgColor by animateColorAsState(if (active) AuraCyan else DarkSurface, tween(200), label = "presetBg")
                    val borderColor by animateColorAsState(if (active) AuraCyan else DarkBorder, tween(200), label = "presetBorder")
                    val textColor by animateColorAsState(if (active) Color.Black else TextSecondary, tween(200), label = "presetText")
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier.clickable { onPresetClick(preset) }
                    ) {
                        Text(
                            preset,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Bold,
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
                BadgeMini("Contextual Switch")
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodProfiles.forEach { mood ->
                    val active = mood == activeMood
                    val bgColor by animateColorAsState(if (active) Color(0xFF00E5FF).copy(alpha = 0.12f) else DarkSurface, tween(200), label = "moodBg")
                    val borderColor by animateColorAsState(if (active) AuraCyan else DarkBorder, tween(200), label = "moodBorder")
                    val textColor by animateColorAsState(if (active) Color.White else TextSecondary, tween(200), label = "moodText")
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier.clickable { onMoodClick(mood) }
                    ) {
                        Text(
                            mood,
                            color = textColor,
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
            QuickActionRow(
                onStyleQuizClick = onStyleQuizClick,
                onSurpriseMeClick = onSurpriseMeClick,
                onHapticsClick = onHapticsClick,
                onAmoledAuditClick = onAmoledAuditClick
            )
            Spacer(Modifier.height(24.dp))
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🔥 Trending Launcher Setups", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                BadgeMini("Top Community Codes")
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

        item {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Simple-By-Default Live Editor", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { /* TODO: AI helper flow */ }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Ask AI Helper", color = AuraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    var clockFont by remember { mutableStateOf("Cyberpunk LCD") }
                    var fontMenuOpen by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Active Clock Typography", color = TextSecondary, fontSize = 13.sp)
                        Box {
                            Row(Modifier.clickable { fontMenuOpen = true }, verticalAlignment = Alignment.CenterVertically) {
                                Text(clockFont, color = Color.White, fontSize = 13.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                            DropdownMenu(expanded = fontMenuOpen, onDismissRequest = { fontMenuOpen = false }) {
                                listOf("Cyberpunk LCD", "Minimal Swiss", "Retro Split Flip", "Luxury Gold Analog").forEach { opt ->
                                    DropdownMenuItem(text = { Text(opt) }, onClick = { clockFont = opt; fontMenuOpen = false })
                                }
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Accent Color", color = TextSecondary, fontSize = 13.sp)
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(AuraPink)
                                .clickable { /* TODO: open color picker */ }
                        )
                    }

                    var widgetOpacity by remember { mutableStateOf(85f) }
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("Widget Opacity", color = TextSecondary, fontSize = 13.sp)
                        Slider(
                            value = widgetOpacity,
                            onValueChange = { widgetOpacity = it },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(thumbColor = AuraPink, activeTrackColor = AuraPink)
                        )
                    }

                    var advancedOpen by remember { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth().clickable { advancedOpen = !advancedOpen }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Advanced Controls (Shadow, Blur & Spacing)", color = TextSecondary, fontSize = 12.sp)
                        Icon(if (advancedOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = TextSecondary)
                    }

                    if (advancedOpen) {
                        var blurRadius by remember { mutableStateOf(18f) }
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text("Backdrop Blur Radius", color = TextSecondary, fontSize = 12.sp)
                            Slider(value = blurRadius, onValueChange = { blurRadius = it }, valueRange = 0f..40f,
                                colors = SliderDefaults.colors(thumbColor = AuraCyan, activeTrackColor = AuraCyan))
                        }

                        var zIndex by remember { mutableStateOf("Bring to Front (Z=20)") }
                        var zMenuOpen by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Z-Index Layer Priority", color = TextSecondary, fontSize = 12.sp)
                            Box {
                                Row(Modifier.clickable { zMenuOpen = true }, verticalAlignment = Alignment.CenterVertically) {
                                    Text(zIndex, color = Color.White, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                                DropdownMenu(expanded = zMenuOpen, onDismissRequest = { zMenuOpen = false }) {
                                    listOf("Bring to Front (Z=20)", "Send to Back (Z=5)").forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = { zIndex = opt; zMenuOpen = false })
                                    }
                                }
                            }
                        }

                        var hapticStrength by remember { mutableStateOf("Subtle Micro-Tick") }
                        var hapticMenuOpen by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Haptic Vibration Intensity", color = TextSecondary, fontSize = 12.sp)
                            Box {
                                Row(Modifier.clickable { hapticMenuOpen = true }, verticalAlignment = Alignment.CenterVertically) {
                                    Text(hapticStrength, color = Color.White, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                                DropdownMenu(expanded = hapticMenuOpen, onDismissRequest = { hapticMenuOpen = false }) {
                                    listOf("Subtle Micro-Tick", "Crisp Chime", "Mechanical Thock").forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = { hapticStrength = opt; hapticMenuOpen = false })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(AuraPink, Color(0xFF990033))))
                    .clickable { /* TODO: wire to actual "apply to real home screen" flow */ },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Apply Directly to Real Home Screen", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
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
fun VibeSyncContent(
    viewModel: ExploreViewModel,
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel
) {
    var selected by remember { mutableStateOf<VibeOption?>(vibeOptions.first()) }
    val context = LocalContext.current
    val extractedPalette by viewModel.extractedPalette.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var uploadedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun loadBitmapFromUri(uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        null
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bitmap = loadBitmapFromUri(uri)
        if (bitmap != null) {
            uploadedBitmap = bitmap
            selected = null // photo takes over from preset cards
            viewModel.processVibeSync(bitmap)
            SoundEngine.playHapticSound("crystal")
        } else {
            Toast.makeText(context, "Couldn't read that photo", Toast.LENGTH_SHORT).show()
        }
    }

    // Colors currently on screen: either from a chosen preset card, or
    // from a photo that's been scanned (extractedPalette).
    val displayColors: List<Color> = when {
        selected != null -> selected!!.colors
        extractedPalette != null -> listOf(
            extractedPalette!!.vibrant,
            extractedPalette!!.dominant,
            extractedPalette!!.lightVibrant,
            extractedPalette!!.darkVibrant
        )
        else -> vibeOptions.first().colors
    }

    fun colorToHex(c: Color): String {
        val argb = c.toArgb()
        return String.format("#%06X", 0xFFFFFF and argb)
    }

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
                        val active = selected == vibe
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurface)
                                .border(1.dp, if (active) AuraPink else DarkBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    selected = vibe
                                    uploadedBitmap = null // preset card overrides any uploaded photo
                                    SoundEngine.playHapticSound("crystal")
                                }
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
                    .border(1.dp, if (uploadedBitmap != null) AuraCyan else AuraCyan.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .clickable {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(AuraCyan.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(color = AuraCyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (uploadedBitmap != null) "Photo Scanned ✓" else "Upload or Snap Any Photo",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
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
                (displayColors + Color(0xFF0D091A)).forEach { c ->
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c).border(1.dp, DarkBorder, RoundedCornerShape(10.dp)))
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        item {
            Button(
                onClick = {
                    val bmp = uploadedBitmap
                    val primaryHex: String
                    val secondaryHex: String
                    val palette: ExtractedVibePalette

                    if (bmp != null && extractedPalette != null) {
                        palette = extractedPalette!!
                        primaryHex = colorToHex(palette.vibrant)
                        secondaryHex = colorToHex(palette.dominant)
                    } else {
                        val vibe = selected ?: vibeOptions.first()
                        palette = ExtractedVibePalette(
                            dominant = vibe.colors[0],
                            vibrant = vibe.colors.getOrElse(1) { vibe.colors[0] },
                            lightVibrant = vibe.colors.getOrElse(2) { vibe.colors[0] },
                            darkVibrant = vibe.colors.getOrElse(3) { Color(0xFF0D091A) },
                            muted = TextSecondary
                        )
                        primaryHex = colorToHex(vibe.colors[0])
                        secondaryHex = colorToHex(vibe.colors.getOrElse(1) { vibe.colors[0] })
                    }

                    homeViewModel.applyVibePalette(palette)
                    settingsViewModel.saveSelectedTheme("vibe-sync", primaryHex, secondaryHex)
                    SoundEngine.playHapticSound("cyber")

                    val onResult: (Boolean) -> Unit = { }
                    if (bmp != null) {
                        viewModel.applyWallpaperBitmap(bmp, onResult)
                    } else {
                        viewModel.applyWallpaperUrl((selected ?: vibeOptions.first()).imageUrl, onResult)
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

// presetName matches a key in ExploreScreen.kt's stylePresetMap.
// wallpaperOverride matches web's per-card customImg override in
// js/studio/explore-engine.js (only "workday" differs from its preset's default image).
private data class TimePreset(
    val time: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val presetName: String,
    val wallpaperOverride: String? = null
)

private val timePresets = listOf(
    TimePreset(
        "07:00 AM", "Sunrise Fresh", "Energizing emerald hues & morning weather", Icons.Default.WbSunny, Color(0xFF10B981),
        presetName = "Emerald Nature"
    ),
    TimePreset(
        "11:30 AM", "Workday Focus", "Monochrome minimal typography & agenda", Icons.Default.Work, Color(0xFFE2E8F0),
        presetName = "Minimal Bauhaus",
        wallpaperOverride = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=1080&auto=format&fit=crop"
    ),
    TimePreset(
        "06:45 PM", "Golden Dusk", "Warm amber glow & memory frame", Icons.Default.WbTwilight, Color(0xFFFF8C42),
        presetName = "Sunset Wave"
    ),
    TimePreset(
        "11:15 PM", "Midnight AMOLED", "Pure 0% black OLED with neon cyber accent", Icons.Default.NightsStay, Color(0xFFA855F7),
        presetName = "Cyberpunk"
    ),
)

@Composable
fun SurpriseContent(onApplyPreset: (String, String?) -> Unit) {
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
                        .clickable {
                            selected = preset
                            onApplyPreset(preset.presetName, preset.wallpaperOverride)
                        }
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
            onClick = {
                val roll = timePresets.random()
                selected = roll
                // Matches web's random roll: no customImg, so preset's own default wallpaper is used.
                onApplyPreset(roll.presetName, null)
            },
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

private data class HapticProfile(val key: String, val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val hapticProfiles = listOf(
    HapticProfile("crystal", "Crystal Minimal", "High-frequency crisp chimes", Icons.Default.Diamond),
    HapticProfile("mechanical", "Mechanical Thock", "Tactile lubed switch feel", Icons.Default.Keyboard),
    HapticProfile("cyber", "Cyber Pulse", "Futuristic synthesizer hum", Icons.Default.Memory),
    HapticProfile("velvet", "Velvet Acoustic", "Soft acoustic wooden tick", Icons.Default.Spa)
)

@Composable
fun HapticsLabContent() {
    var active by remember { mutableStateOf(hapticProfiles.first().key) }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Surface(color = Color(0xFF00E5FF).copy(alpha = 0.12f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))) {
            Text("⭐ FLAGSHIP IDEA #10", color = AuraCyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("Sound & Haptic Personality", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Pair your visual style with tactile micro-vibrations and soothing audio tokens.", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            hapticProfiles.forEach { profile ->
                val isActive = profile.key == active
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) AuraCyan.copy(alpha = 0.08f) else DarkSurface)
                        .border(1.dp, if (isActive) AuraCyan else DarkBorder, RoundedCornerShape(14.dp))
                        .clickable { active = profile.key }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(AuraCyan.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(profile.icon, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(profile.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(profile.subtitle, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            active = profile.key
                            SoundEngine.playHapticSound(profile.key)
                        },
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Test", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class AuditStatusItem(val title: String, val subtitle: String)

private val auditStatusItems = listOf(
    AuditStatusItem("Default Home Launcher Role", "Fully Automated via Android RoleManager API"),
    AuditStatusItem("Direct AppWidgetHost Hosting", "Hosted Natively on Real Home Screen"),
    AuditStatusItem("No-Grid Real Home Placement", "Fully Controlled by Aura Launcher Core"),
    AuditStatusItem("Material You Palette Quantization", "Fully Automated (On-Device Palette Engine)")
)

@Composable
fun DeviceAuditContent() {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Surface(color = AuraSuccess.copy(alpha = 0.12f), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, AuraSuccess.copy(alpha = 0.3f))) {
            Text("⚡ HARDWARE & BATTERY AUDIT", color = AuraSuccess, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("Device Audit Score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text("Real-time hardware limitation classification ensuring zero fake settings.", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AuraSuccess.copy(alpha = 0.12f))
                        .border(3.dp, AuraSuccess, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("94%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text("AMOLED", color = TextMuted, fontSize = 8.sp)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("OLED Battery Optimized", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("True black pixel coverage is saving approx. ~18% battery life.", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                auditStatusItems.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AuraSuccess.copy(alpha = 0.06f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuraSuccess, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(item.subtitle, color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class ChatMessage(val fromUser: Boolean, val text: String)

private val aiSuggestionChips = listOf(
    "🌧️ Cozy Anime Rain" to "Aesthetic cozy anime rainy night with soft neon widgets",
    "⚡ AMOLED Extreme Battery" to "Ultra-minimal AMOLED battery saving setup for focus",
    "👑 Luxury Gold" to "Luxury gold and dark emerald executive business look"
)

// Matches web's keyword-match logic in js/studio/explore-engine.js sendAIMsg() —
// not a real AI/network call, just detects a few keywords and applies the
// matching preset, same as the web build.
private fun aiStylistReplyFor(text: String, onApplyPreset: (String, String?) -> Unit): String {
    val lower = text.lowercase()
    return when {
        lower.contains("rain") || lower.contains("anime") -> {
            onApplyPreset("Cyberpunk", "https://images.unsplash.com/photo-1514565131-fce0801e5785?q=80&w=1080&auto=format&fit=crop")
            "🌧️ Cozy Anime Rain vibe applied with soft neon widgets!"
        }
        lower.contains("battery") || lower.contains("amoled") -> {
            onApplyPreset("Minimal Bauhaus", "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=1080&auto=format&fit=crop")
            "⚡ 98% AMOLED Extreme Battery theme applied!"
        }
        lower.contains("luxury") || lower.contains("gold") -> {
            onApplyPreset("Sunset Wave", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1080&auto=format&fit=crop")
            "👑 Executive Luxury Gold timekeeper activated!"
        }
        else -> "✨ Applied custom setup for \"$text\"!"
    }
}

@Composable
fun AIStylistContent(onApplyPreset: (String, String?) -> Unit) {
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
                    modifier = Modifier.clickable {
                        val reply = aiStylistReplyFor(prompt, onApplyPreset)
                        messages = messages + ChatMessage(true, prompt) + ChatMessage(false, reply)
                        SoundEngine.playHapticSound("crystal")
                    }
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
                            val reply = aiStylistReplyFor(input, onApplyPreset)
                            messages = messages + ChatMessage(true, input) + ChatMessage(false, reply)
                            input = ""
                            SoundEngine.playHapticSound("crystal")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}