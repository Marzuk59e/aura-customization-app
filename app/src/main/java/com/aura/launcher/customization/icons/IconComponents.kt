package com.aura.launcher.customization.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.core.theme.*
import com.aura.launcher.domain.model.AppInfo

enum class IconShape(val label: String) {
    SQUIRCLE("Squircle"),
    CIRCLE("Circle"),
    ROUNDED("Rounded Square"),
    TEARDROP("Teardrop")
}

enum class IconStylePack(val id: String, val title: String, val author: String) {
    DEFAULT("default", "Aura Modern", "Aura Team"),
    NEON_GLOW("neon", "Cyberpunk Neon", "Aura Labs"),
    MINIMAL_MONO("mono", "Minimalist Mono", "Aura Studio"),
    PASTEL_VIBE("pastel", "Soft Pastel", "Community")
}

@Composable
fun getShapeForIcon(shape: IconShape): Shape {
    return when (shape) {
        IconShape.SQUIRCLE -> RoundedCornerShape(18.dp)
        IconShape.CIRCLE -> CircleShape
        IconShape.ROUNDED -> RoundedCornerShape(12.dp)
        IconShape.TEARDROP -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 4.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPackPickerSheet(
    activePack: IconStylePack,
    activeShape: IconShape,
    onDismiss: () -> Unit,
    onSelectPack: (IconStylePack) -> Unit,
    onSelectShape: (IconShape) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Icon Customization", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Icon Shape", style = MaterialTheme.typography.titleMedium, color = AuraPurpleLight)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconShape.values().forEach { shape ->
                    val isSelected = activeShape == shape
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectShape(shape) }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(getShapeForIcon(shape))
                                .background(if (isSelected) AuraPurple else DarkSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) AuraCyan else DarkBorder,
                                    getShapeForIcon(shape)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(shape.label, fontSize = 11.sp, color = if (isSelected) AuraCyan else TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Icon Style Pack", style = MaterialTheme.typography.titleMedium, color = AuraPurpleLight)
            Spacer(modifier = Modifier.height(10.dp))

            IconStylePack.values().forEach { pack ->
                val isSelected = activePack == pack
                Surface(
                    color = if (isSelected) DarkSurfaceVariant else Color.Transparent,
                    shape = RoundedCornerShape(14.dp),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AuraPurpleLight) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectPack(pack) }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(pack.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("By ${pack.author}", color = TextSecondary, fontSize = 12.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AuraCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditAppDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onSave: (newName: String) -> Unit
) {
    var customLabel by remember { mutableStateOf(app.label) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit App Shortcut", color = TextPrimary) },
        text = {
            Column {
                Text("Customize how this application appears:", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraPurpleLight,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(customLabel.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraPurple)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
