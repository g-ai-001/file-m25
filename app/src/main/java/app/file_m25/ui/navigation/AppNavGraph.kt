package app.file_m25.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.file_m25.ui.screens.audiopreview.AudioPreviewScreen
import app.file_m25.ui.screens.file.FileScreen
import app.file_m25.ui.screens.home.HomeScreen
import app.file_m25.ui.screens.imagepreview.ImagePreviewScreen
import app.file_m25.ui.screens.pdfpreview.PdfPreviewScreen
import app.file_m25.ui.screens.settings.SettingsScreen
import app.file_m25.ui.screens.textpreview.TextPreviewScreen
import app.file_m25.ui.screens.trash.TrashScreen
import app.file_m25.ui.screens.videopreview.VideoPreviewScreen

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
                },
                onNavigateToVideoPreview = { path ->
                    navController.navigate(Screen.VideoPreview.createRoute(path))
                },
                onNavigateToAudioPreview = { path ->
                    navController.navigate(Screen.AudioPreview.createRoute(path))
                },
                onNavigateToPdfPreview = { path ->
                    navController.navigate(Screen.PdfPreview.createRoute(path))
                },
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash.route)
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
                },
                onNavigateToVideoPreview = { videoPath ->
                    navController.navigate(Screen.VideoPreview.createRoute(videoPath))
                },
                onNavigateToAudioPreview = { audioPath ->
                    navController.navigate(Screen.AudioPreview.createRoute(audioPath))
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

        composable(
            route = Screen.VideoPreview.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = encodedPath.decodeUrl()
            VideoPreviewScreen(
                videoPath = path,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AudioPreview.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = encodedPath.decodeUrl()
            AudioPreviewScreen(
                audioPath = path,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PdfPreview.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = encodedPath.decodeUrl()
            PdfPreviewScreen(
                pdfPath = path,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Trash.route) {
            TrashScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TextPreview.route,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
            val path = encodedPath.decodeUrl()
            TextPreviewScreen(
                textPath = path,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}