package app.file_m25.domain.model

enum class FileCategory(val displayName: String) {
    IMAGE("图片"),
    VIDEO("视频"),
    AUDIO("音频"),
    DOCUMENT("文档"),
    APP("应用"),
    ARCHIVE("压缩包"),
    OTHER("其他")
}

fun getFileCategory(extension: String, mimeType: String): FileCategory {
    if (extension.isEmpty()) return FileCategory.OTHER
    return when (extension.lowercase()) {
        // 图片
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg", "ico" -> FileCategory.IMAGE
        // 视频
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp" -> FileCategory.VIDEO
        // 音频
        "mp3", "wav", "ogg", "flac", "aac", "wma", "m4a", "opus" -> FileCategory.AUDIO
        // 文档
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp",
        "csv", "xml", "json", "html", "htm", "md" -> FileCategory.DOCUMENT
        // 应用
        "apk", "xapk", "apks", "apkm" -> FileCategory.APP
        // 压缩包
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tar.gz", "tar.bz2" -> FileCategory.ARCHIVE
        else -> FileCategory.OTHER
    }
}

fun getCategoryIcon(category: FileCategory): String {
    return when (category) {
        FileCategory.IMAGE -> "image"
        FileCategory.VIDEO -> "video"
        FileCategory.AUDIO -> "audio"
        FileCategory.DOCUMENT -> "document"
        FileCategory.APP -> "app"
        FileCategory.ARCHIVE -> "archive"
        FileCategory.OTHER -> "file"
    }
}