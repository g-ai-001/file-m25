package app.file_m25.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.file_m25.ui.screens.file.FileScreen
import app.file_m25.ui.screens.home.HomeScreen
import app.file_m25.ui.screens.imagepreview.ImagePreviewScreen
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
                },
                onNavigateToImagePreview = { paths, index ->
                    navController.navigate(Screen.ImagePreview.createRoute(paths, index))
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
                },
                onNavigateToImagePreview = { paths, index ->
                    navController.navigate(Screen.ImagePreview.createRoute(paths, index))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ImagePreview.route,
            arguments = listOf(
                navArgument("paths") { type = NavType.StringType },
                navArgument("initialIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val encodedPaths = backStackEntry.arguments?.getString("paths") ?: ""
            val initialIndex = backStackEntry.arguments?.getInt("initialIndex") ?: 0
            val paths = encodedPaths.split(",").map { it.decodeUrl() }
            ImagePreviewScreen(
                imagePaths = paths,
                initialIndex = initialIndex,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}