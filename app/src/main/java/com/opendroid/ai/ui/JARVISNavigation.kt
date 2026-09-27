package com.opendroid.ai.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aetheria.jarvis.llm.NimTripleClient
import com.opendroid.ai.ui.screens.JARVISSettingsScreen
import com.opendroid.ai.ui.screens.JARVISStatusScreen
import com.opendroid.ai.ui.theme.*
import com.opendroid.ai.ui.viewmodel.SettingsViewModel
import dagger.hilt.navigation.compose.hiltViewModel
import javax.inject.Inject

/**
 * JARVIS Navigation - Minimal UI with just Settings and Status screens
 */
object JARVISRoutes {
    const val SETTINGS = "settings"
    const val STATUS = "status"
}

@Composable
fun JARVISNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = JARVISRoutes.STATUS,
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)
    ) {
        composable(JARVISRoutes.STATUS) {
            JARVISStatusScreen(
                onNavigateToSettings = { navController.navigate(JARVISRoutes.SETTINGS) }
            )
        }

        composable(JARVISRoutes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            JARVISSettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

sealed class JARVISScreen(val route: String, val title: String, val icon: ImageVector) {
    object Status : JARVISScreen("status", "Status", Icons.Default.Dashboard)
    object Settings : JARVISScreen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun JARVISMainDashboard(
    onNavigateToSettings: () -> Unit
) {
    var currentTab by remember { mutableStateOf<JARVISScreen>(JARVISScreen.Status) }
    val tabs = listOf(JARVISScreen.Status, JARVISScreen.Settings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JARVIS", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.surface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = AppTheme.colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .border(1.dp, AppTheme.colors.borderColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            currentTab = tab
                            if (tab == JARVISScreen.Settings) onNavigateToSettings()
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) AppTheme.colors.accentCyan else AppTheme.colors.textSecondary
                            )
                        },
                        label = { Text(tab.title, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppTheme.colors.accentCyan,
                            unselectedIconColor = AppTheme.colors.textSecondary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.98f, animationSpec = tween(200)))
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                }
            ) { tab ->
                when (tab) {
                    JARVISScreen.Status -> JARVISStatusScreen(onNavigateToSettings = onNavigateToSettings)
                    JARVISScreen.Settings -> {
                        // Navigate to settings screen
                        onNavigateToSettings()
                    }
                }
            }
        }
    }
}