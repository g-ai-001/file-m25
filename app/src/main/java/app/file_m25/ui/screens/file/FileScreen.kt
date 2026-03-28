package app.file_m25.ui.screens.file

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import app.file_m25.domain.model.SortMode
import app.file_m25.domain.model.ViewMode
import app.file_m25.ui.components.DeleteConfirmDialog
import app.file_m25.ui.components.EmptyState
import app.file_m25.ui.components.FileGridItem
import app.file_m25.ui.components.FileInfoDialog
import app.file_m25.ui.components.FileListItem
import app.file_m25.ui.components.LoadingIndicator
import app.file_m25.ui.components.RenameDialog
import app.file_m25.ui.components.DestinationPickerDialog
import app.file_m25.ui.screens.home.FileOperationBottomSheet
import app.file_m25.util.formatDate
import app.file_m25.util.formatFileSize
import app.file_m25.util.getSortModeLabel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(
    path: String,
    onNavigateBack: () -> Unit,
    onNavigateToFile: (String) -> Unit,
    onNavigateToImagePreview: (List<String>, Int) -> Unit,
    onNavigateToVideoPreview: (String) -> Unit,
    onNavigateToAudioPreview: (String) -> Unit,
    viewModel: FileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    LaunchedEffect(uiState.shareFilePath) {
        uiState.shareFilePath?.let { path ->
            try {
                val file = File(path)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "分享文件"))
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("分享失败: ${e.message}")
            }
            viewModel.clearShareFile()
        }
    }

    if (uiState.isMultiSelectMode) {
        MultiSelectModeScaffold(
            uiState = uiState,
            onBack = { viewModel.clearSelection() },
            onSelectAll = { viewModel.selectAllFiles() },
            onDelete = { viewModel.showDeleteDialog() },
            onCopy = { viewModel.showCopyDialog() },
            onMove = { viewModel.showMoveDialog() },
            onToggleSelection = { file -> viewModel.toggleFileSelection(file) }
        )
    } else if (uiState.isSearchMode) {
        SearchModeScaffold(
            uiState = uiState,
            onBack = { viewModel.exitSearchMode() },
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onFileClick = { file ->
                if (file.isDirectory) {
                    viewModel.exitSearchMode()
                    onNavigateToFile(file.path)
                } else {
                    viewModel.selectFile(file)
                    viewModel.addToRecent(file)
                }
            }
        )
    } else {
        NormalModeScaffold(
            uiState = uiState,
            path = path,
            showSortMenu = showSortMenu,
            onShowSortMenu = { showSortMenu = true },
            onHideSortMenu = { showSortMenu = false },
            onNavigateBack = onNavigateBack,
            onNavigateToFile = onNavigateToFile,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            onNavigateToImagePreview = onNavigateToImagePreview,
            onNavigateToVideoPreview = onNavigateToVideoPreview,
            onNavigateToAudioPreview = onNavigateToAudioPreview
        )
    }

    uiState.selectedFile?.let { file ->
        FileOperationBottomSheet(
            fileName = file.name,
            isZipFile = file.extension.lowercase() == "zip",
            isFavorite = uiState.isFavorite,
            isBookmarked = uiState.isBookmarked,
            onInfo = { viewModel.showFileInfoDialog() },
            onRename = { viewModel.showRenameDialog() },
            onCopy = { viewModel.showCopyDialog() },
            onMove = { viewModel.showMoveDialog() },
            onDelete = { viewModel.showDeleteDialog() },
            onCompress = { },
            onExtract = { },
            onToggleFavorite = { viewModel.toggleFavorite(file) },
            onToggleBookmark = { viewModel.toggleBookmark(file) },
            onShare = { viewModel.shareFile(file) },
            onDismiss = { viewModel.selectFile(null) }
        )
    }

    if (uiState.showRenameDialog && uiState.selectedFile != null) {
        RenameDialog(
            currentName = uiState.selectedFile!!.name,
            onDismiss = { viewModel.hideRenameDialog() },
            onConfirm = { name -> viewModel.renameFile(name) }
        )
    }

    if (uiState.showDeleteDialog) {
        DeleteConfirmDialog(
            itemName = if (uiState.selectedFiles.isNotEmpty()) {
                "${uiState.selectedFiles.size} 个项目"
            } else {
                uiState.selectedFile?.name ?: ""
            },
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { viewModel.deleteFile() }
        )
    }

    if (uiState.showFileInfoDialog && uiState.selectedFile != null) {
        val file = uiState.selectedFile!!
        FileInfoDialog(
            fileName = file.name,
            filePath = file.path,
            fileSize = if (file.isDirectory) "文件夹" else formatFileSize(file.size),
            fileDate = formatDate(file.lastModified),
            fileType = if (file.isDirectory) "文件夹" else file.extension.uppercase().ifEmpty { "文件" },
            onDismiss = { viewModel.hideFileInfoDialog() }
        )
    }

    if (uiState.showCopyDialog) {
        DestinationPickerDialog(
            title = "复制到",
            currentPath = uiState.currentPath,
            onDismiss = { viewModel.hideCopyDialog() },
            onConfirm = { dest -> viewModel.copyFile(dest) }
        )
    }

    if (uiState.showMoveDialog) {
        DestinationPickerDialog(
            title = "移动到",
            currentPath = uiState.currentPath,
            onDismiss = { viewModel.hideMoveDialog() },
            onConfirm = { dest -> viewModel.moveFile(dest) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalModeScaffold(
    uiState: FileUiState,
    path: String,
    showSortMenu: Boolean,
    onShowSortMenu: () -> Unit,
    onHideSortMenu: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToFile: (String) -> Unit,
    viewModel: FileViewModel,
    snackbarHostState: SnackbarHostState,
    onNavigateToImagePreview: (List<String>, Int) -> Unit,
    onNavigateToVideoPreview: (String) -> Unit,
    onNavigateToAudioPreview: (String) -> Unit
) {
    val pathParts = path.split("/").filter { it.isNotEmpty() }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("文件管理器") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.enterSearchMode() }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        Box {
                            IconButton(onClick = onShowSortMenu) {
                                Icon(Icons.Default.Sort, contentDescription = "排序")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = onHideSortMenu
                            ) {
                                SortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(getSortModeLabel(mode)) },
                                        onClick = {
                                            viewModel.setSortMode(mode)
                                            onHideSortMenu()
                                        },
                                        leadingIcon = {
                                            if (uiState.sortMode == mode) {
                                                Icon(
                                                    Icons.Default.Sort,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(if (uiState.showHiddenFiles) "隐藏文件" else "显示文件") },
                                    onClick = {
                                        viewModel.setShowHiddenFiles(!uiState.showHiddenFiles)
                                        onHideSortMenu()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (uiState.showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                        IconButton(onClick = {
                            viewModel.setViewMode(
                                if (uiState.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                            )
                        }) {
                            Icon(
                                if (uiState.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = "视图模式"
                            )
                        }
                    }
                )
                // 面包屑导航单独一行，支持横向滚动
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    pathParts.forEachIndexed { index, part ->
                        if (index > 0) {
                            Text(">", modifier = Modifier.padding(horizontal = 4.dp))
                        }
                        TextButton(
                            onClick = {
                                if (index < pathParts.size - 1) {
                                    val targetPath = pathParts.take(index + 1).joinToString("/")
                                    onNavigateToFile("/$targetPath")
                                }
                            }
                        ) {
                            Text(
                                text = part,
                                color = if (index == pathParts.size - 1) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> EmptyState(message = uiState.error ?: "未知错误")
                uiState.files.isEmpty() -> EmptyState(message = "文件夹为空")
                else -> {
                    if (uiState.viewMode == ViewMode.LIST) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(uiState.files, key = { it.path }) { file ->
                                FileListItem(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) {
                                            onNavigateToFile(file.path)
                                        } else if (file.mimeType.startsWith("image/")) {
                                            val imageFiles = uiState.files.filter { it.mimeType.startsWith("image/") }
                                            val index = imageFiles.indexOf(file)
                                            onNavigateToImagePreview(imageFiles.map { it.path }, if (index >= 0) index else 0)
                                        } else if (file.mimeType.startsWith("video/")) {
                                            onNavigateToVideoPreview(file.path)
                                        } else if (file.mimeType.startsWith("audio/")) {
                                            onNavigateToAudioPreview(file.path)
                                        } else {
                                            viewModel.selectFile(file)
                                            viewModel.addToRecent(file)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.selectFile(file)
                                    }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.files, key = { it.path }) { file ->
                                FileGridItem(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) {
                                            onNavigateToFile(file.path)
                                        } else if (file.mimeType.startsWith("image/")) {
                                            val imageFiles = uiState.files.filter { it.mimeType.startsWith("image/") }
                                            val index = imageFiles.indexOf(file)
                                            onNavigateToImagePreview(imageFiles.map { it.path }, if (index >= 0) index else 0)
                                        } else if (file.mimeType.startsWith("video/")) {
                                            onNavigateToVideoPreview(file.path)
                                        } else if (file.mimeType.startsWith("audio/")) {
                                            onNavigateToAudioPreview(file.path)
                                        } else {
                                            viewModel.selectFile(file)
                                            viewModel.addToRecent(file)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchModeScaffold(
    uiState: FileUiState,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFileClick: (app.file_m25.domain.model.FileItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("搜索文件...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isSearching -> LoadingIndicator()
                uiState.searchResults.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    EmptyState(message = "未找到匹配的文件")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.searchResults, key = { it.path }) { file ->
                            FileListItem(
                                file = file,
                                onClick = { onFileClick(file) },
                                onLongClick = { }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectModeScaffold(
    uiState: FileUiState,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onToggleSelection: (app.file_m25.domain.model.FileItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已选择 ${uiState.selectedFiles.size} 项") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "取消")
                    }
                },
                actions = {
                    IconButton(onClick = onSelectAll) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "全选")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                    }
                    IconButton(onClick = onMove) {
                        Icon(Icons.Default.MoveUp, contentDescription = "移动")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(uiState.files, key = { it.path }) { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onToggleSelection(file) },
                            onLongClick = { onToggleSelection(file) }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.selectedFiles.contains(file),
                        onCheckedChange = { onToggleSelection(file) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FileListItem(
                        file = file,
                        onClick = { onToggleSelection(file) },
                        onLongClick = { onToggleSelection(file) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}