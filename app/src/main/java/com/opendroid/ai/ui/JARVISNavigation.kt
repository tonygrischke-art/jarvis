package com.opendroid.ai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.opendroid.ai.data.repository.SettingsRepository
import com.opendroid.ai.ui.screens.JARVISSettingsScreen
import com.opendroid.ai.ui.screens.JARVISStatusScreen

sealed class JARVISScreen(val route: String) {
    object Status : JARVISScreen("status")
    object Settings : JARVISScreen("settings")
}

@Composable
fun JARVISNavigation(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = JARVISScreen.Status.route,
        modifier = modifier
    ) {
        composable(JARVISScreen.Status.route) {
            JARVISStatusScreen(
                settingsRepository = settingsRepository,
                modifier = Modifier
            )
        }

        composable(JARVISScreen.Settings.route) {
            JARVISSettingsScreen(
                settingsRepository = settingsRepository,
                modifier = Modifier
            )
        }
    }
}