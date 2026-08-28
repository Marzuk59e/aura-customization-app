package com.aura.launcher.launcher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.launcher.core.theme.*
import com.aura.launcher.domain.model.LauncherSettings
import com.aura.launcher.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<LauncherSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LauncherSettings())

    fun toggleLabels(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleLabels(show)
        }
    }

    fun updateGrid(rows: Int, cols: Int) {
        viewModelScope.launch {
            settingsRepository.updateGridDimensions(rows, cols)
        }
    }

    fun requestSetAsDefault() {
        settingsRepository.requestDefaultLauncher()
    }

    // New wrappers for customization persistence
    fun saveSelectedIconPack(name: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedIconPack(name)
        }
    }

    fun saveSelectedTheme(themeId: String, primaryColor: String, secondaryColor: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedTheme(themeId, primaryColor, secondaryColor)
        }
    }

    fun saveSelectedFont(fontName: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedFont(fontName)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Launcher Settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Default Launcher Prompt Card
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraPurpleLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = AuraCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Default Home App",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Set Aura as your phone's default launcher to unlock seamless gestures and smooth home screen replacement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.requestSetAsDefault() },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set as Default Launcher", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Home Screen Customization", style = MaterialTheme.typography.titleMedium, color = AuraPurpleLight)

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Show App Labels", color = TextPrimary) },
                        supportingContent = { Text("Display text labels underneath app icons", color = TextSecondary) },
                        trailingContent = {
                            Switch(
                                checked = settings.showLabels,
                                onCheckedChange = { viewModel.toggleLabels(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AuraCyan, checkedTrackColor = AuraPurple)
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )

                    HorizontalDivider(color = DarkBorder)

                    ListItem(
                        headlineContent = { Text("Grid Density", color = TextPrimary) },
                        supportingContent = { Text("Current: ${settings.gridRows} rows x ${settings.gridCols} columns", color = TextSecondary) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }
        }
    }
}
