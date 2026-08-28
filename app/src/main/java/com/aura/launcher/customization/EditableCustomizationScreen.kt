package com.aura.launcher.customization

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.core.theme.*
import com.aura.launcher.customization.setups.SavedSetupsViewModel
import com.aura.launcher.customization.explore.ExploreViewModel
import com.aura.launcher.launcher.home.HomeViewModel
import com.aura.launcher.launcher.settings.SettingsViewModel
import com.aura.launcher.customization.vibesync.ExtractedVibePalette
import com.aura.launcher.customization.icons.IconStylePack
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableCustomizationScreen(
    savedSetupsViewModel: SavedSetupsViewModel,
    exploreViewModel: ExploreViewModel,
    homeViewModel: HomeViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Observe repository data
    val wallpapers by exploreViewModel.wallpapers.collectAsState()
    val iconPacks by exploreViewModel.iconPacks.collectAsState()
    val themes by exploreViewModel.themes.collectAsState()

    // Simple selection state per category
    val categories = listOf("Wallpapers", "Themes", "Fonts", "Icons", "Widgets", "Other")
    var activeCategory by remember { mutableStateOf(categories.first()) }

    // store simple string selections for each category
    val selections = remember { mutableStateMapOf<String, String>() }
    categories.forEach { if (selections[it] == null) selections[it] = "None" }

    var showPreview by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editable Customization", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {

            // Category selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    val active = cat == activeCategory
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) AuraPink.copy(alpha = 0.08f) else DarkSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeCategory = cat }
                    ) {
                        Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            val icon = when(cat) {
                                "Wallpapers" -> Icons.Default.Image
                                "Themes" -> Icons.Default.Palette
                                "Fonts" -> Icons.Default.FontDownload
                                "Icons" -> Icons.Default.Apps
                                "Widgets" -> Icons.Default.Widgets
                                else -> Icons.Default.Settings
                            }
                            Icon(icon, contentDescription = null, tint = if (active) AuraPink else TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(cat, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simple options list for active category
            Column(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(DarkSurface, RoundedCornerShape(14.dp))
                .padding(12.dp)) {

                Text("Select $activeCategory", color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // Options pulled from repositories / viewmodels
                val options = when(activeCategory) {
                    "Wallpapers" -> if (wallpapers.isNotEmpty()) wallpapers.map { it.title } else listOf("No wallpapers")
                    "Themes" -> if (themes.isNotEmpty()) themes.map { it.name } else listOf("No themes")
                    "Fonts" -> listOf("Inter", "Roboto", "SF Pro")
                    "Icons" -> if (iconPacks.isNotEmpty()) iconPacks.map { it.name } else listOf("No icon packs")
                    "Widgets" -> listOf("Digital Clock", "Weather Card", "Battery Gauge")
                    else -> listOf("Edge Glow", "Sound FX")
                }

                options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selections[activeCategory] = opt }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(opt, color = TextPrimary)
                        val chosen = selections[activeCategory] == opt
                        if (chosen) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuraPurple)
                    }
                    Divider(color = DarkBorder)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions row: Preview, Save, Apply
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showPreview = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AuraCyan)) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Preview")
                }

                Button(onClick = { showSaveDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AuraPurple)) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }

                Button(onClick = {
                    // Save then apply: save setup and apply wallpaper if selected
                    // Save the setup (title will be auto-generated from selections)
                    savedSetupsViewModel.saveActiveScreenAsSetup("${selections.values.joinToString(", ")}")

                    // Apply Wallpaper
                    val selectedWallpaperTitle = selections["Wallpapers"]
                    if (!selectedWallpaperTitle.isNullOrBlank() && selectedWallpaperTitle != "None" && wallpapers.isNotEmpty()) {
                        val wp = wallpapers.find { it.title == selectedWallpaperTitle }
                        if (wp != null) {
                            exploreViewModel.applyWallpaper(wp) { success ->
                                Toast.makeText(context, if (success) "Wallpaper applied." else "Failed to apply wallpaper.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // Apply Icon Pack (map remote icon name to local IconStylePack enum when possible)
                    val selectedIconName = selections["Icons"]
                    if (!selectedIconName.isNullOrBlank() && selectedIconName != "None" && iconPacks.isNotEmpty()) {
                        val remotePack = iconPacks.find { it.name == selectedIconName }
                        val mappedPack = when {
                            remotePack == null -> IconStylePack.DEFAULT
                            IconStylePack.values().any { it.title.equals(remotePack.name, ignoreCase = true) } -> IconStylePack.values().first { it.title.equals(remotePack.name, ignoreCase = true) }
                            IconStylePack.values().any { remotePack.name.contains(it.title, ignoreCase = true) } -> IconStylePack.values().first { remotePack.name.contains(it.title, ignoreCase = true) }
                            else -> IconStylePack.DEFAULT
                        }
                        val usedFallback = remotePack != null && mappedPack.title != remotePack.name && mappedPack == IconStylePack.DEFAULT
                        homeViewModel.setIconPack(mappedPack)
                        settingsViewModel.saveSelectedIconPack(remotePack?.name ?: mappedPack.title)
                        if (usedFallback) {
                            Toast.makeText(context, "This icon pack isn't fully supported yet, applying closest match.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Icon pack applied: ${mappedPack.title}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Apply Theme
                    val selectedThemeName = selections["Themes"]
                    if (!selectedThemeName.isNullOrBlank() && selectedThemeName != "None" && themes.isNotEmpty()) {
                        val remoteTheme = themes.find { it.name == selectedThemeName }
                        if (remoteTheme != null) {
                            // Convert hex colors to Compose Color
                            val primary = try { ComposeColor(AndroidColor.parseColor(remoteTheme.primaryColor)) } catch (e: Exception) { AuraPurple }
                            val secondary = try { ComposeColor(AndroidColor.parseColor(remoteTheme.secondaryColor)) } catch (e: Exception) { AuraCyan }
                            val palette = ExtractedVibePalette(dominant = primary, vibrant = secondary, lightVibrant = primary, darkVibrant = ComposeColor(AndroidColor.parseColor("#14141C")), muted = TextSecondary)
                            homeViewModel.applyVibePalette(palette)
                            settingsViewModel.saveSelectedTheme(remoteTheme.id, remoteTheme.primaryColor, remoteTheme.secondaryColor)
                            Toast.makeText(context, "Theme applied: ${remoteTheme.name}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Apply Font (persist selection; theme observes DataStore instantly)
                    val selectedFont = selections["Fonts"]
                    if (!selectedFont.isNullOrBlank() && selectedFont != "None") {
                        settingsViewModel.saveSelectedFont(selectedFont)
                        Toast.makeText(context, "Font selection saved: $selectedFont", Toast.LENGTH_SHORT).show()
                    }

                    // Final message
                    Toast.makeText(context, "Customization applied where supported. Some system-level changes may still require permissions.", Toast.LENGTH_LONG).show()

                    onBack()
                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AuraSuccess)) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply")
                }
            }
        }
    }

    if (showPreview) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            title = { Text("Preview", color = TextPrimary) },
            text = {
                Column {
                    Text("Combined preview of selections:", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    selections.forEach { (k, v) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(k, color = TextPrimary)
                            Text(v, color = AuraCyan)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Use Edit to tweak selections before saving or applying.", color = TextMuted)
                }
            },
            confirmButton = {
                Button(onClick = { showPreview = false }, colors = ButtonDefaults.buttonColors(containerColor = AuraPurple)) { Text("Close") }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Design", color = TextPrimary) },
            text = {
                Column {
                    Text("Name this design", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = saveName, onValueChange = { saveName = it }, placeholder = { Text("My Midnight Theme") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    savedSetupsViewModel.saveActiveScreenAsSetup(saveName)
                    Toast.makeText(context, "Design saved", Toast.LENGTH_SHORT).show()
                    saveName = ""
                    showSaveDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = AuraPurple)) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel", color = TextSecondary) }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
