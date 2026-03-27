package app.file_m25.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.file_m25.domain.model.FileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileListItem(
    file: FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = getFileIcon(file),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = getFileIconColor(file)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!file.isDirectory) {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatDate(file.lastModified),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FileGridItem(
    file: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(getFileBackgroundColor(file)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getFileIcon(file),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = getFileIconColor(file)
            )
        }
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun BreadcrumbItem(
    name: String,
    onClick: () -> Unit,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isLast) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onClick)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun getFileIcon(file: FileItem): ImageVector {
    if (file.isDirectory) {
        return Icons.Default.Folder
    }
    return when {
        file.mimeType.startsWith("image/") -> Icons.Default.Image
        file.mimeType.startsWith("audio/") -> Icons.Default.AudioFile
        file.mimeType.startsWith("video/") -> Icons.Default.VideoFile
        file.mimeType == "application/pdf" -> Icons.Default.InsertDriveFile
        else -> Icons.Default.InsertDriveFile
    }
}

fun getFileIconColor(file: FileItem): Color {
    if (file.isDirectory) {
        return Color(0xFFFFA726)
    }
    return when {
        file.mimeType.startsWith("image/") -> Color(0xFF66BB6A)
        file.mimeType.startsWith("audio/") -> Color(0xFFAB47BC)
        file.mimeType.startsWith("video/") -> Color(0xFFEF5350)
        file.mimeType == "application/pdf" -> Color(0xFFE53935)
        else -> Color(0xFF42A5F5)
    }
}

fun getFileBackgroundColor(file: FileItem): Color {
    if (file.isDirectory) {
        return Color(0xFFFFF3E0)
    }
    return when {
        file.mimeType.startsWith("image/") -> Color(0xFFE8F5E9)
        file.mimeType.startsWith("audio/") -> Color(0xFFF3E5F5)
        file.mimeType.startsWith("video/") -> Color(0xFFFFEBEE)
        else -> Color(0xFFE3F2FD)
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = size.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) "${value.toInt()} ${units[unitIndex]}" else "%.1f %s".format(value, units[unitIndex])
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}