package app.file_m25.ui.screens.home

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.file_m25.domain.model.FileItem
import app.file_m25.domain.model.SortMode
import app.file_m25.domain.model.ViewMode
import app.file_m25.domain.repository.StorageInfo
import app.file_m25.domain.usecase.CreateFolderUseCase
import app.file_m25.domain.usecase.DeleteFileUseCase
import app.file_m25.domain.usecase.GetFilesUseCase
import app.file_m25.domain.usecase.GetStorageInfoUseCase
import app.file_m25.domain.usecase.RenameFileUseCase
import app.file_m25.domain.usecase.SearchFilesUseCase
import app.file_m25.domain.usecase.CopyFileUseCase
import app.file_m25.domain.usecase.MoveFileUseCase
import app.file_m25.domain.usecase.CompressFilesUseCase
import app.file_m25.domain.usecase.ExtractZipUseCase
import app.file_m25.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class HomeUiState(
    val currentPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortMode: SortMode = SortMode.NAME_ASC,
    val viewMode: ViewMode = ViewMode.LIST,
    val storageInfo: StorageInfo? = null,
    val selectedFile: FileItem? = null,
    val showCreateFolderDialog: Boolean = false,
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
    val showCompressDialog: Boolean = false,
    val showExtractDialog: Boolean = false,
    val isCompressing: Boolean = false,
    val isExtracting: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFilesUseCase: GetFilesUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val searchFilesUseCase: SearchFilesUseCase,
    private val copyFileUseCase: CopyFileUseCase,
    private val moveFileUseCase: MoveFileUseCase,
    private val compressFilesUseCase: CompressFilesUseCase,
    private val extractZipUseCase: ExtractZipUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
        loadStorageInfo()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getFilesUseCase(_uiState.value.currentPath, _uiState.value.sortMode)
                .catch { e ->
                    Logger.e("HomeViewModel", "Failed to load files", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { files ->
                    _uiState.update { it.copy(isLoading = false, files = files) }
                }
        }
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                val info = getStorageInfoUseCase()
                _uiState.update { it.copy(storageInfo = info) }
            } catch (e: Exception) {
                Logger.e("HomeViewModel", "Failed to load storage info", e)
            }
        }
    }

    fun navigateTo(path: String) {
        _uiState.update { it.copy(currentPath = path) }
        loadFiles()
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

    fun showCreateFolderDialog() {
        _uiState.update { it.copy(showCreateFolderDialog = true) }
    }

    fun hideCreateFolderDialog() {
        _uiState.update { it.copy(showCreateFolderDialog = false) }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = createFolderUseCase(_uiState.value.currentPath, name)
            result.onSuccess {
                Logger.i("HomeViewModel", "Folder created: $name")
                hideCreateFolderDialog()
                loadFiles()
            }.onFailure { e ->
                Logger.e("HomeViewModel", "Failed to create folder", e)
            }
        }
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
                Logger.i("HomeViewModel", "File renamed to: $newName")
                hideRenameDialog()
                selectFile(null)
                loadFiles()
            }.onFailure { e ->
                Logger.e("HomeViewModel", "Failed to rename file", e)
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
                Logger.i("HomeViewModel", "File deleted: ${selectedFile.name}")
                hideDeleteDialog()
                selectFile(null)
                loadFiles()
                loadStorageInfo()
            }.onFailure { e ->
                Logger.e("HomeViewModel", "Failed to delete file", e)
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
                    Logger.e("HomeViewModel", "Search failed", e)
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
                Logger.i("HomeViewModel", "File copied to: $destFolder")
                hideCopyDialog()
                selectFile(null)
                loadFiles()
            }.onFailure { e ->
                Logger.e("HomeViewModel", "Failed to copy file", e)
            }
        }
    }

    fun moveFile(destFolder: String) {
        val sourcePath = _uiState.value.operationSourcePath ?: return
        viewModelScope.launch {
            val result = moveFileUseCase(sourcePath, destFolder)
            result.onSuccess {
                Logger.i("HomeViewModel", "File moved to: $destFolder")
                hideMoveDialog()
                selectFile(null)
                loadFiles()
                loadStorageInfo()
            }.onFailure { e ->
                Logger.e("HomeViewModel", "Failed to move file", e)
            }
        }
    }

    fun showCompressDialog() {
        _uiState.update { it.copy(showCompressDialog = true) }
    }

    fun hideCompressDialog() {
        _uiState.update { it.copy(showCompressDialog = false) }
    }

    fun showExtractDialog() {
        _uiState.update { it.copy(showExtractDialog = true) }
    }

    fun hideExtractDialog() {
        _uiState.update { it.copy(showExtractDialog = false) }
    }

    fun compressFiles(fileName: String) {
        val selectedFile = _uiState.value.selectedFile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCompressing = true) }
            val sourcePaths = listOf(selectedFile.path)
            val destPath = File(_uiState.value.currentPath, "$fileName.zip").absolutePath
            val result = compressFilesUseCase(sourcePaths, destPath)
            result.onSuccess {
                Logger.i("HomeViewModel", "Files compressed to: $destPath")
                hideCompressDialog()
                selectFile(null)
                loadFiles()
            }.onFailure { e ->
                Logger.e("HomeViewModel", "Failed to compress files", e)
            }
            _uiState.update { it.copy(isCompressing = false) }
        }
    }

    fun extractZip(destFolder: String) {
        val selectedFile = _uiState.value.selectedFile ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExtracting = true) }
            val result = extractZipUseCase(selectedFile.path, destFolder)
            result.onSuccess {
                Logger.i("HomeViewModel", "Zip extracted to: $destFolder")
                hideExtractDialog()
                selectFile(null)
                loadFiles()
            }.onFailure { e ->
                Logger.e("HomeViewModel", "Failed to extract zip", e)
            }
            _uiState.update { it.copy(isExtracting = false) }
        }
    }
}