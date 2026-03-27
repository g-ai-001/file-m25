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
import app.file_m25.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val showDeleteDialog: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFilesUseCase: GetFilesUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val getStorageInfoUseCase: GetStorageInfoUseCase
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
}