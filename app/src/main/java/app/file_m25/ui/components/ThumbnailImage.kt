package app.file_m25.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.file_m25.domain.model.FileItem
import app.file_m25.util.Logger
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.io.File

@Composable
fun ThumbnailImage(
    file: FileItem,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(file.path) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(file.path) { mutableStateOf(true) }
    var loadFailed by remember(file.path) { mutableStateOf(false) }

    DisposableEffect(file.path) {
        isLoading = true
        loadFailed = false

        when {
            file.mimeType.startsWith("image/") -> {
                loadImageThumbnail(context, file.path) { result ->
                    bitmap = result
                    isLoading = false
                    if (result == null) loadFailed = true
                }
            }
            file.mimeType.startsWith("video/") -> {
                loadVideoThumbnail(context, file.path) { result ->
                    bitmap = result
                    isLoading = false
                    if (result == null) loadFailed = true
                }
            }
            else -> {
                isLoading = false
                loadFailed = true
            }
        }

        onDispose { }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Crop
                )
            }
            isLoading -> {
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(getFileBackgroundColor(file))
                )
            }
            else -> {
                Icon(
                    imageVector = if (loadFailed) Icons.Default.BrokenImage else Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(size),
                    tint = getFileIconColor(file)
                )
            }
        }
    }
}

private fun loadImageThumbnail(context: Context, path: String, callback: (Bitmap?) -> Unit) {
    try {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(File(path))
            .size(200, 200)
            .crossfade(true)
            .build()

        val result = imageLoader.execute(request)
        if (result != null) {
            callback(result.toBitmap())
        } else {
            callback(null)
        }
    } catch (e: Exception) {
        Logger.e("ThumbnailImage", "Failed to load image thumbnail", e)
        callback(null)
    }
}

private fun loadVideoThumbnail(context: Context, path: String, callback: (Bitmap?) -> Unit) {
    try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ThumbnailUtils.extractThumbnail(
                ThumbnailUtils.createVideoThumbnail(File(path), Size(200, 200), null),
                200, 200
            )
        } else {
            @Suppress("DEPRECATION")
            ThumbnailUtils.createVideoThumbnail(
                path,
                MediaStore.Images.Thumbnails.MINI_KIND
            )
        }
        callback(bitmap)
    } catch (e: Exception) {
        Logger.e("ThumbnailImage", "Failed to load video thumbnail", e)
        callback(null)
    }
}
