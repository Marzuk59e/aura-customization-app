package com.aura.launcher.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.core.theme.AuraCyan
import com.aura.launcher.core.theme.AuraPurple
import com.aura.launcher.core.theme.DarkBg
import com.aura.launcher.core.theme.DarkBorder
import com.aura.launcher.core.theme.DarkSurface
import com.aura.launcher.core.theme.TextPrimary
import com.aura.launcher.core.theme.TextSecondary

/**
 * Shown whenever a guest taps an account-gated feature.
 * - "Login / Sign up" -> [onProceed] (caller opens the login screen)
 * - "Continue browsing" / scrim tap -> [onDismiss] (guest stays in guest mode)
 */
@Composable
fun GuestGateDialog(
    visible: Boolean,
    featureName: String = "this feature",
    onProceed: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg.copy(alpha = 0.72f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(tween(200)),
                exit = scaleOut(targetScale = 0.9f) + fadeOut(tween(150))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                        // Swallow taps so tapping the card doesn't dismiss via the scrim below.
                        .clickable(enabled = false) {}
                        .padding(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AuraCyan, AuraPurple))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 18.dp)
                    ) {
                        Text(
                            text = "Sign in to continue",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "You're browsing as a guest. Log in or create an account to use $featureName.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 22.dp)
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(AuraCyan, AuraPurple)))
                            .clickable(onClick = onProceed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Login / Sign up", color = Color(0xFF04050A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Text(
                        text = "Continue browsing",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .clickable(onClick = onDismiss)
                    )
                }
            }
        }
    }
}
