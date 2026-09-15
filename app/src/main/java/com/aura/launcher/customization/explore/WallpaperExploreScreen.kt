package com.aura.launcher.customization.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.aura.launcher.core.audio.SoundEngine
import com.aura.launcher.core.theme.*
import com.aura.launcher.domain.model.RemoteWallpaper

/**
 * Full-page wallpaper browser, opened from the "Wallpaper" category button
 * on the Explore Ground dashboard. Wallpapers come from the real
 * [com.aura.launcher.data.repository.WallpaperRepositoryImpl] (API-backed,
 * with an offline fallback) — this screen only adds the missing browse UI:
 * filter by category, preview, and apply.
 */
@Composable
fun WallpaperExploreScreen(
    wallpapers: List<RemoteWallpaper>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onApply: (RemoteWallpaper) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var previewWallpaper by remember { mutableStateOf<RemoteWallpaper?>(null) }

    val categories = remember(wallpapers) {
        listOf("All") + wallpapers.map { it.category }.distinct().sorted()
    }
    val filtered = remember(wallpapers, selectedCategory) {
        if (selectedCategory == "All") wallpapers else wallpapers.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Explore Wallpapers", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("${filtered.size} wallpapers${if (selectedCategory != "All") " in $selectedCategory" else ""}", color = TextSecondary, fontSize = 11.sp)
            }
        }

        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val active = category == selectedCategory
                Surface(
                    color = if (active) AuraPink.copy(alpha = 0.15f) else DarkSurface,
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (active) AuraPink else DarkBorder),
                    modifier = Modifier.clickable {
                        selectedCategory = category
                        SoundEngine.playHapticSound("crystal")
                    }
                ) {
                    Text(
                        category,
                        color = if (active) AuraPink else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(color = AuraPink, modifier = Modifier.align(Alignment.Center))
            } else if (filtered.isEmpty()) {
                Text("No wallpapers found in this category.", color = TextMuted, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { wp ->
                        WallpaperGridTile(wallpaper = wp, onClick = { previewWallpaper = wp })
                    }
                }
            }
        }
    }

    previewWallpaper?.let { wp ->
        WallpaperPreviewDialog(
            wallpaper = wp,
            onDismiss = { previewWallpaper = null },
            onApply = {
                onApply(wp)
                previewWallpaper = null
            }
        )
    }
}

@Composable
private fun WallpaperGridTile(wallpaper: RemoteWallpaper, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)), startY = 90f))
            )
            if (wallpaper.tier != "free") {
                Surface(
                    color = AuraAmber.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Text(wallpaper.tier.uppercase(), color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(
                wallpaper.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
        }
    }
}

@Composable
private fun WallpaperPreviewDialog(
    wallpaper: RemoteWallpaper,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                AsyncImage(
                    model = wallpaper.imageUrl,
                    contentDescription = wallpaper.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)), startY = 250f))
                )
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(wallpaper.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${wallpaper.category} • ${wallpaper.downloads} downloads", color = AuraCyan, fontSize = 11.sp)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Close", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(AuraPink, Color(0xFF990033))))
                        .clickable { onApply() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply Wallpaper", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}