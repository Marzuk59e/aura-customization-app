package com.aura.launcher.launcher.home

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aura.launcher.core.utils.rememberAppIconBitmap
import com.aura.launcher.core.theme.*
import com.aura.launcher.customization.icons.IconShape
import com.aura.launcher.customization.icons.getShapeForIcon
import com.aura.launcher.domain.model.HomeItem
import com.aura.launcher.domain.model.HomeItemType
import kotlin.math.roundToInt

@Composable
fun HomeDock(
    dockItems: List<HomeItem>,
    iconShape: IconShape = IconShape.SQUIRCLE,
    accentColor: Color = AuraCyan,
    onItemClick: (HomeItem) -> Unit,
    onOpenAppDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Matches css/components/dock.css: .dock-apps-container (glass pill, --bg-glass-dock)
    Surface(
        color = Color(0xBF0A0D14), // --bg-glass-dock: rgba(10,13,20,0.75)
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val left = dockItems.take(2)
            val right = dockItems.drop(2).take(2)

            left.forEach { item ->
                DockIconItem(item = item, iconShape = iconShape, accentColor = accentColor, onClick = { onItemClick(item) })
            }

            // Center: App Drawer trigger — matches .drawer-bg gradient (accent-primary -> #a855f7)
            AppDrawerButton(onClick = onOpenAppDrawer)

            right.forEach { item ->
                DockIconItem(item = item, iconShape = iconShape, accentColor = accentColor, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
fun AppDrawerButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(AuraPink, AuraPurple)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = "Open app drawer",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun DockIconItem(
    item: HomeItem,
    iconShape: IconShape = IconShape.SQUIRCLE,
    accentColor: Color = AuraCyan,
    onClick: () -> Unit
) {
    val iconBitmap: Bitmap? = rememberAppIconBitmap(item.packageName, item.activityName)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(getShapeForIcon(iconShape))
            .background(Brush.linearGradient(listOf(AuraPurple, accentColor)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap.asImageBitmap(),
                contentDescription = item.label,
                modifier = Modifier.size(36.dp)
            )
        } else {
            Text(
                text = item.label?.take(1)?.uppercase() ?: "A",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun HomeGridItem(
    item: HomeItem,
    modifier: Modifier = Modifier,
    iconShape: IconShape = IconShape.SQUIRCLE,
    accentColor: Color = AuraPurpleLight,
    isEditMode: Boolean = false,
    isDragging: Boolean = false,
    dragOffset: Offset = Offset.Zero,
    onDelete: () -> Unit = {},
    onPositioned: (Rect) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onClick: () -> Unit
) {
    val iconBitmap: Bitmap? = rememberAppIconBitmap(item.packageName, item.activityName)

    // Small alternating tilt while editing — the industry-standard "these
    // icons are now editable" cue (every mainstream launcher does this).
    // Bounds collapse to 0 when not editing, so this infinite animation is
    // cheap and harmless outside edit mode rather than needing to be
    // conditionally created (Compose disallows conditional hook calls).
    val infiniteTransition = rememberInfiniteTransition(label = "iconJiggle")
    val jiggleAngle by infiniteTransition.animateFloat(
        initialValue = if (isEditMode) -1f else 0f,
        targetValue = if (isEditMode) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleAngle"
    )
    // Alternate tilt direction by id so a grid of icons doesn't jiggle in
    // perfect unison — a small touch that reads as much more organic.
    val jiggleSign = remember(item.id) { if (item.id % 2L == 0L) 1f else -1f }

    Column(
        modifier = modifier
            .onGloballyPositioned { coords -> onPositioned(coords.boundsInWindow()) }
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .scale(if (isDragging) 1.12f else 1f)
            .graphicsLayer { rotationZ = jiggleAngle * jiggleSign * 1.5f }
            .shadow(if (isDragging) 20.dp else 0.dp, RoundedCornerShape(14.dp))
            .zIndex(if (isDragging) 1f else 0f)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isEditMode) {
                    Modifier.pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, amount ->
                                change.consume()
                                onDrag(amount)
                            }
                        )
                    }
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (item.itemType == HomeItemType.FOLDER) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(getShapeForIcon(iconShape))
                        .background(DarkSurfaceVariant)
                        .background(Brush.radialGradient(listOf(AuraPurple.copy(alpha = 0.35f), Color.Transparent)))
                        .border(1.dp, AuraPurpleLight.copy(alpha = 0.5f), getShapeForIcon(iconShape)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = item.label,
                        tint = AuraCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(getShapeForIcon(iconShape))
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap.asImageBitmap(),
                            contentDescription = item.label,
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        Text(
                            text = item.label?.take(1)?.uppercase() ?: "A",
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // In Edit Mode, show a glowing remove badge — scales/fades in
            // rather than instantly popping, matching the spring-pop language
            // used for CTA buttons and success checkmarks elsewhere in the app.
            EditModeDeleteBadge(
                visible = isEditMode,
                onDelete = onDelete
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.label ?: "App",
            color = TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EditModeDeleteBadge(
    visible: Boolean,
    onDelete: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(),
        exit = scaleOut(tween(120)) + fadeOut(tween(120))
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .offset(x = 4.dp, y = (-4).dp)
                .clip(CircleShape)
                .background(AuraPink)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}