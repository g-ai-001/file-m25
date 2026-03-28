package app.file_m25.domain.model

import java.io.File

data class FileItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String = "",
    val mimeType: String = "",
    val category: FileCategory = if (isDirectory) FileCategory.OTHER else getFileCategory(extension, mimeType)
) {
    val nameWithoutExtension: String
        get() = if (extension.isNotEmpty()) {
            name.removeSuffix(".$extension")
        } else {
            name
        }

    companion object {
        fun fromFile(file: File): FileItem {
            val ext = if (file.isFile) file.extension.lowercase() else ""
            val mime = getMimeType(file)
            return FileItem(
                path = file.absolutePath,
                name = file.name,
                isDirectory = file.isDirectory,
                size = if (file.isFile) file.length() else 0L,
                lastModified = file.lastModified(),
                extension = ext,
                mimeType = mime,
                category = if (file.isDirectory) FileCategory.OTHER else getFileCategory(ext, mime)
            )
        }

        private fun getMimeType(file: File): String {
            if (file.isDirectory) return ""
            val ext = file.extension.lowercase()
            return when (ext) {
                "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image/*"
                "mp3", "wav", "ogg", "flac", "aac" -> "audio/*"
                "mp4", "mkv", "avi", "mov", "wmv" -> "video/*"
                "pdf" -> "application/pdf"
                "txt", "log" -> "text/plain"
                "html", "htm" -> "text/html"
                "css" -> "text/css"
                "js" -> "application/javascript"
                "json" -> "application/json"
                "xml" -> "application/xml"
                "zip", "rar", "7z", "tar", "gz" -> "application/zip"
                "doc", "docx" -> "application/msword"
                "xls", "xlsx" -> "application/vnd.ms-excel"
                "ppt", "pptx" -> "application/vnd.ms-powerpoint"
                else -> "*/*"
            }
        }
    }
}

enum class SortMode {
    NAME_ASC,
    NAME_DESC,
    SIZE_ASC,
    SIZE_DESC,
    DATE_ASC,
    DATE_DESC
}

enum class ViewMode {
    LIST,
    GRID
}