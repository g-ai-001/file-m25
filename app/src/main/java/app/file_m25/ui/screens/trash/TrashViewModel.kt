package app.file_m25.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.file_m25.data.repository.TrashRepository
import app.file_m25.domain.model.FileItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedFile: FileItem? = null,
    val showRestoreDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showEmptyTrashDialog: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadTrash()
    }

    private fun loadTrash() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                trashRepository.getAllTrashItems().collect { files ->
                    _uiState.update { it.copy(files = files, isLoading = false, error = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun selectFile(file: FileItem?) {
        _uiState.update { it.copy(selectedFile = file) }
    }

    fun showRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = true) }
    }

    fun hideRestoreDialog() {
        _uiState.update { it.copy(showRestoreDialog = false) }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun showEmptyTrashDialog() {
        _uiState.update { it.copy(showEmptyTrashDialog = true) }
    }

    fun hideEmptyTrashDialog() {
        _uiState.update { it.copy(showEmptyTrashDialog = false) }
    }

    fun restoreFile() {
        viewModelScope.launch {
            val file = _uiState.value.selectedFile ?: return@launch
            try {
                val success = trashRepository.restoreFromTrash(file.path)
                if (success) {
                    _uiState.update { it.copy(snackbarMessage = "已恢复到原位置", showRestoreDialog = false, selectedFile = null) }
                } else {
                    _uiState.update { it.copy(snackbarMessage = "恢复失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "恢复失败: ${e.message}", showRestoreDialog = false) }
            }
        }
    }

    fun permanentlyDeleteFile() {
        viewModelScope.launch {
            val file = _uiState.value.selectedFile ?: return@launch
            try {
                val success = trashRepository.deleteFromTrash(file.path)
                if (success) {
                    _uiState.update { it.copy(snackbarMessage = "已彻底删除", showDeleteDialog = false, selectedFile = null) }
                } else {
                    _uiState.update { it.copy(snackbarMessage = "删除失败") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "删除失败: ${e.message}", showDeleteDialog = false) }
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            try {
                trashRepository.emptyTrash()
                _uiState.update { it.copy(snackbarMessage = "回收站已清空", showEmptyTrashDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "清空失败: ${e.message}", showEmptyTrashDialog = false) }
            }
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}