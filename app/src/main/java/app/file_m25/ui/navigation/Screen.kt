package app.file_m25.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object File : Screen("file/{path}") {
        fun createRoute(path: String) = "file/${path.encodeUrl()}"
    }
    data object Settings : Screen("settings")
}

fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, "UTF-8")

fun String.decodeUrl(): String = java.net.URLDecoder.decode(this, "UTF-8")