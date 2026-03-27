package app.file_m25.ui.screens.file

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.file_m25.domain.model.FileItem
import app.file_m25.domain.model.SortMode
import app.file_m25.domain.model.ViewMode
import app.file_m25.domain.usecase.DeleteFileUseCase
import app.file_m25.domain.usecase.GetFilesUseCase
import app.file_m25.domain.usecase.RenameFileUseCase
import app.file_m25.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileUiState(
    val currentPath: String = "",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortMode: SortMode = SortMode.NAME_ASC,
    val viewMode: ViewMode = ViewMode.LIST,
    val selectedFile: FileItem? = null,
    val showRenameDialog: Boolean = false,
    val showDeleteDialog: Boolean = false
)

@HiltViewModel
class FileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFilesUseCase: GetFilesUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val deleteFileUseCase: DeleteFileUseCase
) : ViewModel() {

    private val encodedPath: String = savedStateHandle.get<String>("path") ?: ""

    private val _uiState = MutableStateFlow(FileUiState())
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()

    init {
        val path = java.net.URLDecoder.decode(encodedPath, "UTF-8")
        _uiState.update { it.copy(currentPath = path) }
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getFilesUseCase(_uiState.value.currentPath, _uiState.value.sortMode)
                .catch { e ->
                    Logger.e("FileViewModel", "Failed to load files", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { files ->
                    _uiState.update { it.copy(isLoading = false, files = files) }
                }
        }
    }

    fun setSortMode(mode: SortMode) {
        _uiState.update { it.copy(sortMode = mode) }
        loadFiles()
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun selectFile(file: FileItem?) {
        _uiState.update { it.copy(selectedFile = file) }
    }

    fun showRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = true) }
    }

    fun hideRenameDialog() {
        _uiState.update { it.copy(showRenameDialog = false) }
    }

    fun renameFile(newName: String) {
        val selectedFile = _uiState.value.selectedFile ?: return
        viewModelScope.launch {
            val result = renameFileUseCase(selectedFile.path, newName)
            result.onSuccess {
                Logger.i("FileViewModel", "File renamed to: $newName")
                hideRenameDialog()
                selectFile(null)
                loadFiles()
            }.onFailure { e ->
                Logger.e("FileViewModel", "Failed to rename file", e)
            }
        }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteFile() {
        val selectedFile = _uiState.value.selectedFile ?: return
        viewModelScope.launch {
            val result = deleteFileUseCase(selectedFile.path)
            result.onSuccess {
                Logger.i("FileViewModel", "File deleted: ${selectedFile.name}")
                hideDeleteDialog()
                selectFile(null)
                loadFiles()
            }.onFailure { e ->
                Logger.e("FileViewModel", "Failed to delete file", e)
            }
        }
    }
}