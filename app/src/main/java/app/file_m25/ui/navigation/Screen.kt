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
    data object VideoPreview : Screen("video_preview/{path}") {
        fun createRoute(path: String) = "video_preview/${path.encodeUrl()}"
    }
    data object AudioPreview : Screen("audio_preview/{path}") {
        fun createRoute(path: String) = "audio_preview/${path.encodeUrl()}"
    }
    data object PdfPreview : Screen("pdf_preview/{path}") {
        fun createRoute(path: String) = "pdf_preview/${path.encodeUrl()}"
    }
    data object Trash : Screen("trash")
}

fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, "UTF-8")

fun String.decodeUrl(): String = java.net.URLDecoder.decode(this, "UTF-8")
