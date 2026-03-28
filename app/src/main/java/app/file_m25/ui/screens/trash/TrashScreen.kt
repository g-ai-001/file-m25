package app.file_m25.ui.screens.trash

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.file_m25.ui.components.DeleteConfirmDialog
import app.file_m25.ui.components.EmptyState
import app.file_m25.ui.components.FileListItem
import app.file_m25.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.files.isNotEmpty()) {
                        IconButton(onClick = { viewModel.showEmptyTrashDialog() }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "清空回收站")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.files.isNotEmpty()) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = { viewModel.showRestoreDialog() },
                            enabled = uiState.selectedFile != null
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = "恢复")
                        }
                        IconButton(
                            onClick = { viewModel.showDeleteDialog() },
                            enabled = uiState.selectedFile != null
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "彻底删除")
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
                uiState.files.isEmpty() -> EmptyState(message = "回收站是空的")
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.files, key = { it.path }) { file ->
                            TrashFileItem(
                                file = file,
                                isSelected = uiState.selectedFile == file,
                                onClick = { viewModel.selectFile(file) },
                                onLongClick = { viewModel.selectFile(file) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showRestoreDialog && uiState.selectedFile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRestoreDialog() },
            title = { Text("恢复文件") },
            text = { Text("确定要恢复 ${uiState.selectedFile?.name} 到原位置吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreFile() }) {
                    Text("恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRestoreDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    if (uiState.showDeleteDialog && uiState.selectedFile != null) {
        DeleteConfirmDialog(
            itemName = uiState.selectedFile?.name ?: "",
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { viewModel.permanentlyDeleteFile() }
        )
    }

    if (uiState.showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideEmptyTrashDialog() },
            title = { Text("清空回收站") },
            text = { Text("确定要清空回收站吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyTrash() }) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideEmptyTrashDialog() }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashFileItem(
    file: app.file_m25.domain.model.FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FileListItem(
            file = file,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier.weight(1f)
        )
    }
}