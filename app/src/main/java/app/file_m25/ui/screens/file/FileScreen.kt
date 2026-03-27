package app.file_m25.ui.screens.file

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.file_m25.domain.model.SortMode
import app.file_m25.domain.model.ViewMode
import app.file_m25.ui.components.DeleteConfirmDialog
import app.file_m25.ui.components.EmptyState
import app.file_m25.ui.components.FileGridItem
import app.file_m25.ui.components.FileListItem
import app.file_m25.ui.components.LoadingIndicator
import app.file_m25.ui.components.RenameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScreen(
    path: String,
    onNavigateBack: () -> Unit,
    onNavigateToFile: (String) -> Unit,
    viewModel: FileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var showPathMenu by remember { mutableStateOf(false) }

    val pathParts = path.split("/").filter { it.isNotEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = pathParts.lastOrNull() ?: "文件",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showPathMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showPathMenu,
                            onDismissRequest = { showPathMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("路径: $path") },
                                onClick = { },
                                enabled = false
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "排序")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(getSortModeLabel(mode)) },
                                onClick = {
                                    viewModel.setSortMode(mode)
                                    showSortMenu = false
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
                                        } else {
                                            viewModel.selectFile(file)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.selectFile(file)
                                    },
                                    modifier = Modifier.combinedClickable(
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

    uiState.selectedFile?.let { file ->
        FileOperationBottomSheet(
            fileName = file.name,
            onRename = { viewModel.showRenameDialog() },
            onDelete = { viewModel.showDeleteDialog() },
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

    if (uiState.showDeleteDialog && uiState.selectedFile != null) {
        DeleteConfirmDialog(
            itemName = uiState.selectedFile!!.name,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { viewModel.deleteFile() }
        )
    }
}

private fun getSortModeLabel(mode: SortMode): String {
    return when (mode) {
        SortMode.NAME_ASC -> "名称升序"
        SortMode.NAME_DESC -> "名称降序"
        SortMode.SIZE_ASC -> "大小升序"
        SortMode.SIZE_DESC -> "大小降序"
        SortMode.DATE_ASC -> "日期升序"
        SortMode.DATE_DESC -> "日期降序"
    }
}