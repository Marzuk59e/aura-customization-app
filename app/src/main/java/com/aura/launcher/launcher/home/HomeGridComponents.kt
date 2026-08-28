package com.aura.launcher.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.launcher.core.theme.*
import com.aura.launcher.customization.icons.IconShape
import com.aura.launcher.customization.icons.getShapeForIcon
import com.aura.launcher.domain.model.HomeItem

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
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
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
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(getShapeForIcon(iconShape))
            .background(Brush.linearGradient(listOf(AuraPurple, accentColor)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.label?.take(1)?.uppercase() ?: "A",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
fun HomeGridItem(
    item: HomeItem,
    iconShape: IconShape = IconShape.SQUIRCLE,
    accentColor: Color = AuraPurpleLight,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(getShapeForIcon(iconShape))
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.label?.take(1)?.uppercase() ?: "A",
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
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