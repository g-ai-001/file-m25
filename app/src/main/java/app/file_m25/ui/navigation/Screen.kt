package app.file_m25.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object File : Screen("file/{path}") {
        fun createRoute(path: String) = "file/${path.encodeUrl()}"
    }
    data object Settings : Screen("settings")
    data object ImagePreview : Screen("image_preview/{paths}/{initialIndex}") {
        fun createRoute(paths: List<String>, initialIndex: Int): String {
            val encodedPaths = paths.joinToString(",") { it.encodeUrl() }
            return "image_preview/${encodedPaths}/$initialIndex"
        }
    }
}

fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, "UTF-8")

fun String.decodeUrl(): String = java.net.URLDecoder.decode(this, "UTF-8")