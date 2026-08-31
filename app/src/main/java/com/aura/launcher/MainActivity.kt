package com.aura.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.aura.launcher.auth.*
import com.aura.launcher.core.theme.AuraLauncherTheme
import com.aura.launcher.core.viewmodel.ViewModelFactory
import com.aura.launcher.customization.explore.*
import com.aura.launcher.customization.setups.*
import com.aura.launcher.customization.EditableCustomizationScreen
import com.aura.launcher.launcher.appdrawer.*
import com.aura.launcher.launcher.home.*
import com.aura.launcher.launcher.settings.*

enum class LauncherScreen {
    HOME,
    APP_DRAWER,
    SETTINGS,
    EXPLORE,
    EDITABLE_CUSTOMIZATION,
    SAVED_SETUPS,
    FAVORITES,
    PROFILE,
    WELCOME,
    LOGIN,
    SIGNUP
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val app = application as AuraLauncherApp
        val factory = ViewModelFactory(app)

        val homeViewModel: HomeViewModel by viewModels { factory }
        val appDrawerViewModel: AppDrawerViewModel by viewModels { factory }
        val authViewModel: AuthViewModel by viewModels { factory }
        val savedSetupsViewModel: SavedSetupsViewModel by viewModels { factory }
        val settingsViewModel: SettingsViewModel by viewModels { factory }
        val exploreViewModel: ExploreViewModel by viewModels { factory }

        setContent {
            AuraLauncherTheme {
                val coroutineScope = rememberCoroutineScope()
                var currentScreen by remember { mutableStateOf(LauncherScreen.HOME) }

                val currentUser by authViewModel.currentUser.collectAsState()
                val hasSeenWelcome by app.launcherPreferences.hasSeenWelcomeFlow.collectAsState(initial = false)

                LaunchedEffect(currentUser, hasSeenWelcome) {
                    currentScreen = if (currentUser != null) LauncherScreen.EXPLORE
                    else if (hasSeenWelcome) LauncherScreen.LOGIN
                    else LauncherScreen.WELCOME
                }

                // Intercept back button when not in Home screen
                BackHandler(enabled = currentScreen != LauncherScreen.HOME) {
                    currentScreen = LauncherScreen.HOME
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            LauncherScreen.HOME -> HomeScreen(
                                homeViewModel = homeViewModel,
                                onOpenAppDrawer = { currentScreen = LauncherScreen.APP_DRAWER },
                                onOpenExplore = { currentScreen = LauncherScreen.EXPLORE },
                                onOpenWallpaper = {
                                    exploreViewModel.selectTab(ExploreTab.WALLPAPERS)
                                    currentScreen = LauncherScreen.EXPLORE
                                },
                                onOpenSavedSetups = { currentScreen = LauncherScreen.SAVED_SETUPS },
                                onOpenProfile = { currentScreen = LauncherScreen.PROFILE },
                                onOpenSettings = { currentScreen = LauncherScreen.SETTINGS }
                            )
                            LauncherScreen.APP_DRAWER -> AppDrawerScreen(
                                viewModel = appDrawerViewModel,
                                onCloseDrawer = { currentScreen = LauncherScreen.HOME }
                            )
                            LauncherScreen.EDITABLE_CUSTOMIZATION -> EditableCustomizationScreen(
                                savedSetupsViewModel = savedSetupsViewModel,
                                exploreViewModel = exploreViewModel,
                                homeViewModel = homeViewModel,
                                settingsViewModel = settingsViewModel,
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                            LauncherScreen.EXPLORE -> ExploreScreen(
                                viewModel = exploreViewModel,
                                homeViewModel = homeViewModel,
                                settingsViewModel = settingsViewModel,
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                            LauncherScreen.SETTINGS -> SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                            LauncherScreen.SAVED_SETUPS -> SavedSetupsScreen(
                                viewModel = savedSetupsViewModel,
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                            LauncherScreen.FAVORITES -> FavoritesScreen(
                                viewModel = savedSetupsViewModel,
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                            LauncherScreen.PROFILE -> ProfileScreen(
                                user = currentUser,
                                onNavigateToLogin = { currentScreen = LauncherScreen.LOGIN },
                                onNavigateToSavedSetups = { currentScreen = LauncherScreen.SAVED_SETUPS },
                                onNavigateToFavorites = { currentScreen = LauncherScreen.FAVORITES },
                                onLogout = { authViewModel.logout() },
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                            LauncherScreen.WELCOME -> WelcomeScreen(
                                onGetStarted = {
                                    coroutineScope.launch {
                                        app.launcherPreferences.setHasSeenWelcome(true)
                                        currentScreen = LauncherScreen.LOGIN
                                    }
                                }
                            )
                            LauncherScreen.LOGIN -> LoginScreen(
                                authViewModel = authViewModel,
                                onNavigateToSignUp = { currentScreen = LauncherScreen.SIGNUP },
                                onAuthSuccess = { currentScreen = LauncherScreen.EXPLORE },
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                            LauncherScreen.SIGNUP -> SignUpScreen(
                                authViewModel = authViewModel,
                                onNavigateToLogin = { currentScreen = LauncherScreen.LOGIN },
                                onAuthSuccess = { currentScreen = LauncherScreen.EXPLORE },
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as AuraLauncherApp).appWidgetHostHelper.startListening()
    }

    override fun onStop() {
        super.onStop()
        (application as AuraLauncherApp).appWidgetHostHelper.stopListening()
    }
}