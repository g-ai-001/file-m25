package app.file_m25.ui.screens.file

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.file_m25.data.repository.FavoriteRepository
import app.file_m25.domain.model.FileItem
import app.file_m25.domain.model.SortMode
import app.file_m25.domain.model.ViewMode
import app.file_m25.domain.usecase.DeleteFileUseCase
import app.file_m25.domain.usecase.GetFilesUseCase
import app.file_m25.domain.usecase.RenameFileUseCase
import app.file_m25.domain.usecase.SearchFilesUseCase
import app.file_m25.domain.usecase.CopyFileUseCase
import app.file_m25.domain.usecase.MoveFileUseCase
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
    val showDeleteDialog: Boolean = false,
    val isSearchMode: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val selectedFiles: Set<FileItem> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val showCopyDialog: Boolean = false,
    val showMoveDialog: Boolean = false,
    val operationSourcePath: String? = null,
    val showFileInfoDialog: Boolean = false,
    val snackbarMessage: String? = null,
    val isFavorite: Boolean = false
)

@HiltViewModel
class FileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFilesUseCase: GetFilesUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val searchFilesUseCase: SearchFilesUseCase,
    private val copyFileUseCase: CopyFileUseCase,
    private val moveFileUseCase: MoveFileUseCase,
    private val favoriteRepository: FavoriteRepository
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
        _uiState.update { it.copy(selectedFile = file, isFavorite = false) }
        file?.let { checkFavoriteStatus(it) }
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
                showError("重命名失败: ${e.message}")
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
                showError("删除失败: ${e.message}")
            }
        }
    }

    fun enterSearchMode() {
        _uiState.update { it.copy(isSearchMode = true) }
    }

    fun exitSearchMode() {
        _uiState.update { it.copy(isSearchMode = false, searchQuery = "", searchResults = emptyList()) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isNotBlank()) {
            searchFiles(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        }
    }

    private fun searchFiles(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            searchFilesUseCase(query, _uiState.value.currentPath)
                .catch { e ->
                    Logger.e("FileViewModel", "Search failed", e)
                    _uiState.update { it.copy(isSearching = false) }
                }
                .collect { results ->
                    _uiState.update { it.copy(searchResults = results, isSearching = false) }
                }
        }
    }

    fun toggleMultiSelectMode() {
        _uiState.update {
            it.copy(
                isMultiSelectMode = !it.isMultiSelectMode,
                selectedFiles = if (it.isMultiSelectMode) emptySet() else it.selectedFiles
            )
        }
    }

    fun toggleFileSelection(file: FileItem) {
        _uiState.update { state ->
            val newSelection = if (state.selectedFiles.contains(file)) {
                state.selectedFiles - file
            } else {
                state.selectedFiles + file
            }
            state.copy(selectedFiles = newSelection)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFiles = emptySet(), isMultiSelectMode = false) }
    }

    fun showFileInfoDialog() {
        _uiState.update { it.copy(showFileInfoDialog = true) }
    }

    fun hideFileInfoDialog() {
        _uiState.update { it.copy(showFileInfoDialog = false) }
    }

    fun showCopyDialog(file: FileItem? = null) {
        val path = file?.path ?: _uiState.value.selectedFile?.path ?: return
        _uiState.update { it.copy(showCopyDialog = true, operationSourcePath = path) }
    }

    fun hideCopyDialog() {
        _uiState.update { it.copy(showCopyDialog = false, operationSourcePath = null) }
    }

    fun showMoveDialog(file: FileItem? = null) {
        val path = file?.path ?: _uiState.value.selectedFile?.path ?: return
        _uiState.update { it.copy(showMoveDialog = true, operationSourcePath = path) }
    }

    fun hideMoveDialog() {
        _uiState.update { it.copy(showMoveDialog = false, operationSourcePath = null) }
    }

    fun copyFile(destFolder: String) {
        val sourcePath = _uiState.value.operationSourcePath ?: return
        viewModelScope.launch {
            val result = copyFileUseCase(sourcePath, destFolder)
            result.onSuccess {
                Logger.i("FileViewModel", "File copied to: $destFolder")
                hideCopyDialog()
                selectFile(null)
                loadFiles()
            }.onFailure { e ->
                Logger.e("FileViewModel", "Failed to copy file", e)
                showError("复制失败: ${e.message}")
            }
        }
    }

    fun moveFile(destFolder: String) {
        val sourcePath = _uiState.value.operationSourcePath ?: return
        viewModelScope.launch {
            val result = moveFileUseCase(sourcePath, destFolder)
            result.onSuccess {
                Logger.i("FileViewModel", "File moved to: $destFolder")
                hideMoveDialog()
                selectFile(null)
                loadFiles()
            }.onFailure { e ->
                Logger.e("FileViewModel", "Failed to move file", e)
                showError("移动失败: ${e.message}")
            }
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun toggleFavorite(file: FileItem) {
        viewModelScope.launch {
            if (_uiState.value.isFavorite) {
                favoriteRepository.removeFavorite(file.path)
                Logger.i("FileViewModel", "Removed from favorites: ${file.name}")
            } else {
                favoriteRepository.addFavorite(file)
                Logger.i("FileViewModel", "Added to favorites: ${file.name}")
            }
        }
    }

    fun checkFavoriteStatus(file: FileItem) {
        viewModelScope.launch {
            favoriteRepository.isFavorite(file.path).collect { isFav ->
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
    }
}