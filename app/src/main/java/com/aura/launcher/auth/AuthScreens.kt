package com.aura.launcher.auth

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.core.theme.*
import com.aura.launcher.domain.model.User

// NOTE: LoginScreen and SignUpScreen used to live in this file. They have been
// replaced by the single approved flip-card screen in AuraAuthScreen.kt.
// ProfileScreen (below) is unrelated to the login/signup UI and is preserved as-is.

@Composable
fun ProfileScreen(
    user: User?,
    onNavigateToLogin: () -> Unit,
    onNavigateToSavedSetups: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = "Aura Account",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User Info Card
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AuraPurple, AuraPink))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.displayName?.take(1) ?: "G").uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = user?.displayName ?: "Guest User",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (user?.isGuest == true) "Local Guest Mode" else user?.email ?: "Not signed in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Navigation Items
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("My Saved Setups", color = TextPrimary) },
                        supportingContent = { Text("Browse and apply saved launcher designs", color = TextSecondary) },
                        leadingContent = {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = AuraCyan)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onNavigateToSavedSetups)
                    )

                    HorizontalDivider(color = DarkBorder)

                    ListItem(
                        headlineContent = { Text("My Favorites", color = TextPrimary) },
                        supportingContent = { Text("Favorited wallpapers, icons & presets", color = TextSecondary) },
                        leadingContent = {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = AuraPink)
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(onClick = onNavigateToFavorites)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (user?.isGuest != false) {
                Button(
                    onClick = onNavigateToLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraPurple),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Sign In or Register", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraPink),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraPink.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
