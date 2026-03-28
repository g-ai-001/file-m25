package app.file_m25.ui.screens.textpreview

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextPreviewScreen(
    textPath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var fontSize by remember { mutableFloatStateOf(14f) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentSearchIndex by remember { mutableIntStateOf(0) }
    var totalLines by remember { mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    DisposableEffect(textPath) {
        scope.launch {
            try {
                val file = File(textPath)
                if (!file.exists()) {
                    error = "文件不存在"
                    isLoading = false
                    return@launch
                }

                val fileSize = file.length()
                if (fileSize > 10 * 1024 * 1024) {
                    error = "文件过大，无法预览"
                    isLoading = false
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    val content = file.readText()
                    lines = content.split("\n")
                    totalLines = lines.size
                }
                isLoading = false
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
                isLoading = false
            }
        }
        onDispose { }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            searchResults = lines.mapIndexedNotNull { index, line ->
                if (line.contains(searchQuery, ignoreCase = true)) index else null
            }
            currentSearchIndex = 0
            if (searchResults.isNotEmpty()) {
                scope.launch {
                    listState.animateScrollToItem(searchResults[0])
                }
            }
        } else {
            searchResults = emptyList()
        }
    }

    fun navigateToSearchResult(direction: Int) {
        if (searchResults.isEmpty()) return
        currentSearchIndex = (currentSearchIndex + direction + searchResults.size) % searchResults.size
        scope.launch {
            listState.animateScrollToItem(searchResults[currentSearchIndex])
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = if (totalLines > 0) "${totalLines} 行" else "",
                            color = Color.White
                        )
                    }
                },
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
                    if (showSearch && searchResults.isNotEmpty()) {
                        IconButton(onClick = { navigateToSearchResult(-1) }) {
                            Icon(Icons.Default.TextDecrease, contentDescription = "上一个", tint = Color.White)
                        }
                        Text(
                            text = "${if (searchResults.isNotEmpty()) currentSearchIndex + 1 else 0}/${searchResults.size}",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { navigateToSearchResult(1) }) {
                            Icon(Icons.Default.TextIncrease, contentDescription = "下一个", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { fontSize = (fontSize - 2).coerceAtLeast(10f) }) {
                        Icon(
                            Icons.Default.TextDecrease,
                            contentDescription = "减小字体",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { fontSize = (fontSize + 2).coerceAtMost(32f) }) {
                        Icon(
                            Icons.Default.TextIncrease,
                            contentDescription = "增大字体",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = {
                        try {
                            val file = File(textPath)
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "分享文本"))
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
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(lines.size) { index ->
                            val line = lines[index]
                            val isSearchResult = searchResults.contains(index)
                            val highlightStart = if (isSearchResult && searchQuery.isNotEmpty()) {
                                line.indexOf(searchQuery, ignoreCase = true)
                            } else -1

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSearchResult && currentSearchIndex == searchResults.indexOf(index)) {
                                            Color.DarkGray
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.Gray,
                                    fontSize = fontSize.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                if (highlightStart >= 0) {
                                    val before = line.substring(0, highlightStart)
                                    val match = line.substring(highlightStart, highlightStart + searchQuery.length)
                                    val after = line.substring(highlightStart + searchQuery.length)
                                    Text(
                                        text = before,
                                        color = Color.White,
                                        fontSize = fontSize.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = match,
                                        color = Color.Yellow,
                                        fontSize = fontSize.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = after,
                                        color = Color.White,
                                        fontSize = fontSize.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    Text(
                                        text = line.ifEmpty { " " },
                                        color = Color.White,
                                        fontSize = fontSize.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}