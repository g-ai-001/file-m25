package app.file_m25.ui.screens.pdfpreview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    pdfPath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(0) }
    var scale by remember { mutableFloatStateOf(1f) }
    var showPageList by remember { mutableStateOf(false) }

    val pdfiumCore = remember { PdfiumCore(context) }
    var pdfDocument by remember { mutableStateOf<PdfDocument?>(null) }
    var pageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    DisposableEffect(pdfPath) {
        try {
            val file = File(pdfPath)
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfDocument = pdfiumCore.newDocument(fd)
            pageCount = pdfiumCore.getPageCount(pdfDocument!!)

            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }

        onDispose {
            try {
                pdfDocument?.close()
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(pdfDocument, currentPage, scale) {
        if (pdfDocument != null && pageCount > 0) {
            withContext(Dispatchers.IO) {
                try {
                    val bitmap = renderPage(pdfiumCore, pdfDocument!!, currentPage, scale)
                    pageBitmaps = listOf(bitmap)
                } catch (_: Exception) { }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pageCount > 0) "${currentPage + 1} / $pageCount" else "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showPageList = !showPageList }) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "缩放",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = {
                        try {
                            val file = File(pdfPath)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "分享PDF"))
                        } catch (_: Exception) { }
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "分享",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = error ?: "加载失败",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                showPageList -> {
                    PageListView(
                        pageCount = pageCount,
                        currentPage = currentPage,
                        pageBitmaps = pageBitmaps,
                        pdfDocument = pdfDocument,
                        pdfiumCore = pdfiumCore,
                        scale = scale,
                        onPageSelect = { page ->
                            currentPage = page
                            showPageList = false
                        },
                        onScaleChange = { newScale ->
                            scale = newScale
                        },
                        onBack = { showPageList = false }
                    )
                }
                else -> {
                    ZoomablePdfView(
                        bitmap = pageBitmaps.firstOrNull(),
                        scale = scale,
                        onScaleChange = { newScale ->
                            scale = newScale.coerceIn(0.5f, 5f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomablePdfView(
    bitmap: Bitmap?,
    scale: Float,
    onScaleChange: (Float) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.5f, 5f)
                    onScaleChange(newScale)
                    if (newScale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "PDF页面",
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun PageListView(
    pageCount: Int,
    currentPage: Int,
    pageBitmaps: List<Bitmap>,
    pdfDocument: PdfDocument?,
    pdfiumCore: PdfiumCore,
    scale: Float,
    onPageSelect: (Int) -> Unit,
    onScaleChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }
            Text(
                text = "缩放: ${(scale * 100).toInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ZoomOut,
                contentDescription = "缩小",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Slider(
                value = scale,
                onValueChange = onScaleChange,
                valueRange = 0.5f..3f,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ZoomIn,
                contentDescription = "放大",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items((0 until pageCount).toList()) { page ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "第 ${page + 1} 页",
                        color = if (page == currentPage) MaterialTheme.colorScheme.primary else Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    )
                    if (page == currentPage && pageBitmaps.isNotEmpty()) {
                        pageBitmaps.firstOrNull()?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "页面预览",
                                modifier = Modifier
                                    .height(60.dp)
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (page == currentPage) "当前" else "点击查看",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private suspend fun renderPage(
    pdfiumCore: PdfiumCore,
    document: PdfDocument,
    pageIndex: Int,
    scale: Float
): Bitmap = withContext(Dispatchers.IO) {
    val page = pdfiumCore.getPage(document, pageIndex)
    val width = (page.width * scale).toInt()
    val height = (page.height * scale).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    pdfiumCore.renderPage(document, page, bitmap, width, height, 0, 0)
    bitmap
}