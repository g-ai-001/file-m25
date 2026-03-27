package app.file_m25.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import app.file_m25.ui.components.CreateFolderDialog
import app.file_m25.ui.components.DeleteConfirmDialog
import app.file_m25.ui.components.EmptyState
import app.file_m25.ui.components.FileGridItem
import app.file_m25.ui.components.DestinationPickerDialog
import app.file_m25.ui.components.FileInfoDialog
import app.file_m25.ui.components.FileListItem
import app.file_m25.ui.components.LoadingIndicator
import app.file_m25.ui.components.RenameDialog
import app.file_m25.ui.components.CompressDialog
import app.file_m25.ui.components.StorageIndicator
import app.file_m25.util.formatDate
import app.file_m25.util.formatFileSize
import app.file_m25.util.getSortModeLabel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToFile: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
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

    when {
        uiState.isMultiSelectMode -> {
            MultiSelectModeScaffold(
                uiState = uiState,
                onBack = { viewModel.clearSelection() },
                onSelectAll = { /* TODO */ },
                onDelete = { viewModel.showDeleteDialog() },
                onCopy = { viewModel.showCopyDialog() },
                onMove = { viewModel.showMoveDialog() }
            )
        }
        uiState.isSearchMode -> {
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
        }
        uiState.showFavorites -> {
            FavoritesModeScaffold(
                uiState = uiState,
                onBack = { viewModel.exitFavoritesMode() },
                onNavigateToFile = onNavigateToFile,
                onNavigateToSettings = onNavigateToSettings,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
        uiState.showRecent -> {
            RecentModeScaffold(
                uiState = uiState,
                onBack = { viewModel.exitRecentMode() },
                onNavigateToFile = onNavigateToFile,
                onNavigateToSettings = onNavigateToSettings,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
        else -> {
            NormalModeScaffold(
                uiState = uiState,
                showSortMenu = showSortMenu,
                onShowSortMenu = { showSortMenu = true },
                onHideSortMenu = { showSortMenu = false },
                onNavigateToFile = onNavigateToFile,
                onNavigateToSettings = onNavigateToSettings,
                onEnterFavorites = { viewModel.enterFavoritesMode() },
                onEnterRecent = { viewModel.enterRecentMode() },
                viewModel = viewModel,
                snackbarHostState = snackbarHostState
            )
        }
    }

    uiState.selectedFile?.let { file ->
        FileOperationBottomSheet(
            fileName = file.name,
            isZipFile = file.extension.lowercase() == "zip",
            isFavorite = viewModel.isFavorite(file.path),
            onInfo = { viewModel.showFileInfoDialog() },
            onRename = { viewModel.showRenameDialog() },
            onCopy = { viewModel.showCopyDialog() },
            onMove = { viewModel.showMoveDialog() },
            onDelete = { viewModel.showDeleteDialog() },
            onCompress = { viewModel.showCompressDialog() },
            onExtract = { viewModel.showExtractDialog() },
            onToggleFavorite = { viewModel.toggleFavorite(file) },
            onShare = { viewModel.shareFile(file) },
            onDismiss = { viewModel.selectFile(null) }
        )
    }

    if (uiState.showCreateFolderDialog) {
        CreateFolderDialog(
            currentPath = uiState.currentPath,
            onDismiss = { viewModel.hideCreateFolderDialog() },
            onConfirm = { name -> viewModel.createFolder(name) }
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

    if (uiState.showCompressDialog && uiState.selectedFile != null) {
        CompressDialog(
            currentName = uiState.selectedFile!!.nameWithoutExtension,
            onDismiss = { viewModel.hideCompressDialog() },
            onConfirm = { name -> viewModel.compressFiles(name) }
        )
    }

    if (uiState.showExtractDialog) {
        DestinationPickerDialog(
            title = "解压到",
            currentPath = uiState.currentPath,
            onDismiss = { viewModel.hideExtractDialog() },
            onConfirm = { dest -> viewModel.extractZip(dest) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NormalModeScaffold(
    uiState: HomeUiState,
    showSortMenu: Boolean,
    onShowSortMenu: () -> Unit,
    onHideSortMenu: () -> Unit,
    onNavigateToFile: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onEnterFavorites: () -> Unit,
    onEnterRecent: () -> Unit,
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件管理器") },
                actions = {
                    IconButton(onClick = { viewModel.enterSearchMode() }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onEnterRecent) {
                        Icon(Icons.Default.History, contentDescription = "最近")
                    }
                    IconButton(onClick = onEnterFavorites) {
                        Icon(Icons.Default.Star, contentDescription = "收藏")
                    }
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
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateFolderDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "新建文件夹")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            uiState.storageInfo?.let { info ->
                StorageIndicator(
                    usedSpace = info.usedSpace,
                    totalSpace = info.totalSpace
                )
            }

            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> EmptyState(message = uiState.error ?: "未知错误")
                uiState.files.isEmpty() -> EmptyState(message = "暂无文件")
                else -> {
                    if (uiState.viewMode == ViewMode.LIST) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(uiState.files, key = { it.path }) { file ->
                                FileListItem(
                                    file = file,
                                    onClick = {
                                        if (file.isDirectory) {
                                            onNavigateToFile(file.path)
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
private fun FavoritesModeScaffold(
    uiState: HomeUiState,
    onBack: () -> Unit,
    onNavigateToFile: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
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
                uiState.files.isEmpty() -> EmptyState(message = "暂无收藏")
                else -> {
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
                                    } else {
                                        viewModel.selectFile(file)
                                    }
                                },
                                onLongClick = {
                                    viewModel.selectFile(file)
                                }
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
private fun RecentModeScaffold(
    uiState: HomeUiState,
    onBack: () -> Unit,
    onNavigateToFile: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("最近打开") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
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
                uiState.files.isEmpty() -> EmptyState(message = "暂无最近文件")
                else -> {
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
                                    } else {
                                        viewModel.selectFile(file)
                                    }
                                },
                                onLongClick = {
                                    viewModel.selectFile(file)
                                }
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
private fun SearchModeScaffold(
    uiState: HomeUiState,
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
    uiState: HomeUiState,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit
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
                            onClick = { /* toggle selection */ },
                            onLongClick = { /* toggle selection */ }
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.selectedFiles.contains(file),
                        onCheckedChange = { /* toggle */ }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FileListItem(
                        file = file,
                        onClick = { },
                        onLongClick = { },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

}