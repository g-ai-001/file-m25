package app.file_m25.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.file_m25.ui.screens.file.FileScreen
import app.file_m25.ui.screens.home.HomeScreen
import app.file_m25.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToFile = { path ->
                    navController.navigate(Screen.File.createRoute(path))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.File.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = encodedPath.decodeUrl()
            FileScreen(
                path = path,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFile = { newPath ->
                    navController.navigate(Screen.File.createRoute(newPath))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}