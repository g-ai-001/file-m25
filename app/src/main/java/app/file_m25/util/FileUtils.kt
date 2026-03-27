package app.file_m25.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import app.file_m25.domain.model.SortMode

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    val index = digitGroups.coerceAtMost(units.size - 1)
    return String.format(
        Locale.getDefault(),
        "%.1f %s",
        size / Math.pow(1024.0, index.toDouble()),
        units[index]
    )
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getSortModeLabel(mode: SortMode): String {
    return when (mode) {
        SortMode.NAME_ASC -> "名称升序"
        SortMode.NAME_DESC -> "名称降序"
        SortMode.SIZE_ASC -> "大小升序"
        SortMode.SIZE_DESC -> "大小降序"
        SortMode.DATE_ASC -> "日期升序"
        SortMode.DATE_DESC -> "日期降序"
    }
}