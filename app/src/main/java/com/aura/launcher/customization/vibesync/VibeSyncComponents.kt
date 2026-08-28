package com.aura.launcher.customization.vibesync

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.aura.launcher.core.theme.*

data class ExtractedVibePalette(
    val dominant: Color = AuraPurple,
    val vibrant: Color = AuraCyan,
    val darkVibrant: Color = Color(0xFF14141C),
    val lightVibrant: Color = AuraPurpleLight,
    val muted: Color = TextSecondary
)

object VibeSyncEngine {
    fun extractPalette(bitmap: Bitmap?): ExtractedVibePalette {
        if (bitmap == null) return ExtractedVibePalette()
        return try {
            val palette = Palette.from(bitmap).generate()
            ExtractedVibePalette(
                dominant = palette.dominantSwatch?.rgb?.let { Color(it) } ?: AuraPurple,
                vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) } ?: AuraCyan,
                darkVibrant = palette.darkVibrantSwatch?.rgb?.let { Color(it) } ?: Color(0xFF14141C),
                lightVibrant = palette.lightVibrantSwatch?.rgb?.let { Color(it) } ?: AuraPurpleLight,
                muted = palette.mutedSwatch?.rgb?.let { Color(it) } ?: TextSecondary
            )
        } catch (e: Exception) {
            ExtractedVibePalette()
        }
    }
}

@Composable
fun VibeSyncDialog(
    currentPalette: ExtractedVibePalette,
    onDismiss: () -> Unit,
    onApplyPalette: (ExtractedVibePalette) -> Unit
) {
    var selectedAccent by remember { mutableStateOf(currentPalette.vibrant) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AuraPink)
                Spacer(modifier = Modifier.width(10.dp))
                Text("AI Vibe Sync", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Colors extracted harmoniously from your current background:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Color Swatches Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val swatches = listOf(
                        "Vibrant" to currentPalette.vibrant,
                        "Dominant" to currentPalette.dominant,
                        "Light" to currentPalette.lightVibrant,
                        "Muted" to currentPalette.muted
                    )

                    swatches.forEach { (name, color) ->
                        val isSelected = selectedAccent == color
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedAccent = color }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        2.dp,
                                        if (isSelected) Color.White else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(name, fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyPalette(currentPalette.copy(vibrant = selectedAccent))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraPurple)
            ) {
                Text("Apply Vibe Sync")
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
