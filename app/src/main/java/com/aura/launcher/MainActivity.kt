package com.aura.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
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
    LOGIN
}

class MainActivity : FragmentActivity() {

    private val app by lazy { application as AuraLauncherApp }

    override fun onStart() {
        super.onStart()
        app.appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        app.appWidgetHost.stopListening()
    }

    // singleTask launchMode means a re-tapped deep link (app already running)
    // arrives here instead of a fresh onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        PasswordRecoveryLink.fromDeepLink(intent?.data)?.let { link ->
            PasswordRecoveryLinkHolder.publish(link)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleIncomingIntent(intent)

        val factory = ViewModelFactory(app)

        val homeViewModel: HomeViewModel by viewModels { factory }
        val appDrawerViewModel: AppDrawerViewModel by viewModels { factory }
        val authViewModel: AuthViewModel by viewModels { factory }
        val savedSetupsViewModel: SavedSetupsViewModel by viewModels { factory }
        val settingsViewModel: SettingsViewModel by viewModels { factory }
        val exploreViewModel: ExploreViewModel by viewModels { factory }

        setContent {
            AuraLauncherTheme {
                var currentScreen by remember { mutableStateOf(LauncherScreen.LOGIN) }

                val currentUser by authViewModel.currentUser.collectAsState()

                LaunchedEffect(currentUser) {
                    currentScreen = if (currentUser != null) LauncherScreen.HOME
                    else LauncherScreen.LOGIN
                }

                var showGuestGate by remember { mutableStateOf(false) }
                var guestGateFeatureName by remember { mutableStateOf("this feature") }

                // Guests can browse Home freely, but tapping an account-tied feature
                // (Explore/customization, Wallpapers, Saved Setups, Profile) shows this
                // gate instead of navigating straight there.
                fun requireAuth(featureName: String, action: () -> Unit) {
                    if (currentUser?.isGuest == true) {
                        guestGateFeatureName = featureName
                        showGuestGate = true
                    } else {
                        action()
                    }
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
                                onOpenExplore = {
                                    requireAuth("Explore") { currentScreen = LauncherScreen.EXPLORE }
                                },
                                onOpenWallpaper = {
                                    requireAuth("Wallpapers") {
                                        exploreViewModel.selectTab(ExploreTab.WALLPAPERS)
                                        currentScreen = LauncherScreen.EXPLORE
                                    }
                                },
                                onOpenSavedSetups = {
                                    requireAuth("Saved Setups") { currentScreen = LauncherScreen.SAVED_SETUPS }
                                },
                                onOpenProfile = {
                                    requireAuth("Profile") { currentScreen = LauncherScreen.PROFILE }
                                },
                                onOpenSettings = { currentScreen = LauncherScreen.SETTINGS },
                                isLoggedIn = currentUser != null && currentUser?.isGuest == false
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
                            LauncherScreen.LOGIN -> AuraAuthScreen(
                                authViewModel = authViewModel,
                                onAuthSuccess = { currentScreen = LauncherScreen.HOME },
                                onBack = { currentScreen = LauncherScreen.HOME }
                            )
                        }
                    }

                    GuestGateDialog(
                        visible = showGuestGate,
                        featureName = guestGateFeatureName,
                        onProceed = {
                            showGuestGate = false
                            currentScreen = LauncherScreen.LOGIN
                        },
                        onDismiss = { showGuestGate = false }
                    )
                }
            }
        }
    }
}
