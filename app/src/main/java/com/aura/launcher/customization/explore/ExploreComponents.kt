package com.aura.launcher.customization.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aura.launcher.core.theme.*
import com.aura.launcher.domain.model.*

data class ExploreCategory(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tab: ExploreTab?
)

val exploreCategories = listOf(
    ExploreCategory("Wallpaper", Icons.Default.Image, ExploreTab.WALLPAPERS),
    ExploreCategory("Clock", Icons.Default.Schedule, null),
    ExploreCategory("Widgets", Icons.Default.Widgets, null),
    ExploreCategory("Icons", Icons.Default.Apps, ExploreTab.ICON_PACKS),
    ExploreCategory("Themes", Icons.Default.Palette, ExploreTab.THEMES),
    ExploreCategory("Effects", Icons.Default.AutoAwesome, null),
    ExploreCategory("Haptics", Icons.Default.Vibration, null),
    ExploreCategory("Modes", Icons.Default.Tune, null),
)

val stylePresets = listOf("Cyberpunk", "Minimal Bauhaus", "True AMOLED", "Sunset Wave")
val moodProfiles = listOf("Focus Mode", "Weekend Chill", "Night Gaming")

@Composable
fun CategoryGrid(
    categories: List<ExploreCategory>,
    activeCategory: ExploreCategory?,
    onClick: (ExploreCategory) -> Unit
) {
    categories.chunked(4).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { cat ->
                val active = cat == activeCategory
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) AuraPink.copy(alpha = 0.08f) else DarkSurface)
                        .border(1.dp, if (active) AuraPink else DarkBorder, RoundedCornerShape(16.dp))
                        .clickable { onClick(cat) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(cat.icon, contentDescription = null, tint = if (active) AuraPink else TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(cat.label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun QuickActionRow() {
    data class QuickAction(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)
    val actions = listOf(
        QuickAction("Style Quiz", "Instant Setup", Icons.Default.AutoAwesome, AuraPurple),
        QuickAction("Surprise Me", "Flagship #6", Icons.Default.Shuffle, AuraPink),
        QuickAction("Haptics", "Flagship #10", Icons.Default.Vibration, AuraCyan),
        QuickAction("94% AMOLED", "Audit", Icons.Default.BatteryChargingFull, AuraSuccess)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        actions.forEach { action ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .clickable { }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(action.color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(action.icon, contentDescription = null, tint = action.color, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(action.title, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(action.subtitle, color = TextMuted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
fun TrendingCard(rank: Int, wallpaper: RemoteWallpaper, onImport: () -> Unit) {
    Column(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(90.dp)) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
            ) {
                Text("#$rank TRENDING", color = AuraCyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(wallpaper.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text("AURA-${wallpaper.id.uppercase()}", color = TextMuted, fontSize = 9.sp)
            Spacer(Modifier.height(8.dp))
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
               modifier = Modifier.fillMaxWidth().clickable { onImport() }
            ) {
                Text("Import Setup", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(4.dp))
            }
        }
    }
}

@Composable
fun WallpaperCard(
    wallpaper: RemoteWallpaper,
    onApply: () -> Unit
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = wallpaper.thumbnailUrl,
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 150f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = wallpaper.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = wallpaper.category, color = AuraCyan, fontSize = 11.sp)
                    IconButton(
                        onClick = onApply,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AuraPurple)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Apply", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun IconPackCard(
    iconPack: RemoteIconPack,
    onApply: () -> Unit
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(AuraPurple, AuraPink))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Widgets, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = iconPack.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${iconPack.iconCount} icons • ${iconPack.author}", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = iconPack.description, color = TextMuted, fontSize = 11.sp, maxLines = 2)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = AuraPurple),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Get", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ThemeCard(
    theme: RemoteTheme,
    onApply: () -> Unit
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = theme.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = theme.description, color = TextSecondary, fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(android.graphics.Color.parseColor(theme.primaryColor))))
                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(android.graphics.Color.parseColor(theme.secondaryColor))))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = AuraPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Text("Apply Complete Theme", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
