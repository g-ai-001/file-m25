package app.file_m25.ui.screens.home

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.file_m25.data.repository.FavoriteRepository
import app.file_m25.data.repository.PreferencesRepository
import app.file_m25.data.repository.RecentRepository
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val showHiddenFiles: Boolean = false,
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
    val isExtracting: Boolean = false,
    val snackbarMessage: String? = null,
    val favoritePaths: Set<String> = emptySet(),
    val showFavorites: Boolean = false,
    val showRecent: Boolean = false,
    val shareFilePath: String? = null,
    val bookmarkPaths: Set<String> = emptySet(),
    val showBookmarks: Boolean = false
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
    private val extractZipUseCase: ExtractZipUseCase,
    private val favoriteRepository: FavoriteRepository,
    private val recentRepository: RecentRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var favoritesJob: kotlinx.coroutines.Job? = null

    init {
        loadFiles()
        loadStorageInfo()
        observeFavorites()
        observeShowHiddenFiles()
        observeBookmarks()
    }

    private fun observeShowHiddenFiles() {
        viewModelScope.launch {
            preferencesRepository.showHiddenFiles.collect { show ->
                _uiState.update { it.copy(showHiddenFiles = show) }
                loadFiles()
            }
        }
    }

    private fun observeFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            favoriteRepository.getAllFavorites().collect { favorites ->
                _uiState.update { it.copy(favoritePaths = favorites.map { f -> f.path }.toSet()) }
            }
        }
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            preferencesRepository.bookmarks.collect { bookmarks ->
                _uiState.update { it.copy(bookmarkPaths = bookmarks) }
            }
        }
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val showHidden = _uiState.value.showHiddenFiles
            getFilesUseCase(_uiState.value.currentPath, _uiState.value.sortMode, showHidden)
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

    fun setShowHiddenFiles(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowHiddenFiles(show)
        }
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
                showError("创建文件夹失败: ${e.message}")
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
                Logger.i("HomeViewModel", "File deleted: ${selectedFile.name}")
                hideDeleteDialog()
                selectFile(null)
                loadFiles()
                loadStorageInfo()
            }.onFailure { e ->
                Logger.e("HomeViewModel", "Failed to delete file", e)
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

    fun selectAllFiles() {
        _uiState.update { state ->
            if (state.selectedFiles.size == state.files.size) {
                state.copy(selectedFiles = emptySet())
            } else {
                state.copy(selectedFiles = state.files.toSet())
            }
        }
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
                showError("复制失败: ${e.message}")
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
                showError("移动失败: ${e.message}")
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

    fun showError(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
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
                showError("压缩失败: ${e.message}")
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
                showError("解压失败: ${e.message}")
            }
            _uiState.update { it.copy(isExtracting = false) }
        }
    }

    fun toggleFavorite(file: FileItem) {
        viewModelScope.launch {
            if (_uiState.value.favoritePaths.contains(file.path)) {
                favoriteRepository.removeFavorite(file.path)
                Logger.i("HomeViewModel", "Removed from favorites: ${file.name}")
            } else {
                favoriteRepository.addFavorite(file)
                Logger.i("HomeViewModel", "Added to favorites: ${file.name}")
            }
        }
    }

    fun isFavorite(path: String): Boolean {
        return _uiState.value.favoritePaths.contains(path)
    }

    fun toggleBookmark(file: FileItem) {
        viewModelScope.launch {
            if (_uiState.value.bookmarkPaths.contains(file.path)) {
                preferencesRepository.removeBookmark(file.path)
                Logger.i("HomeViewModel", "Removed bookmark: ${file.name}")
            } else {
                preferencesRepository.addBookmark(file.path)
                Logger.i("HomeViewModel", "Added bookmark: ${file.name}")
            }
        }
    }

    fun isBookmarked(path: String): Boolean {
        return _uiState.value.bookmarkPaths.contains(path)
    }

    fun enterBookmarksMode() {
        _uiState.update { it.copy(showBookmarks = true) }
        loadBookmarks()
    }

    fun exitBookmarksMode() {
        _uiState.update { it.copy(showBookmarks = false) }
        loadFiles()
    }

    private fun loadBookmarks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val bookmarkPaths = _uiState.value.bookmarkPaths
            val files = bookmarkPaths.mapNotNull { path ->
                val file = File(path)
                if (file.exists()) FileItem.fromFile(file) else null
            }
            _uiState.update { it.copy(isLoading = false, files = files) }
        }
    }

    fun enterFavoritesMode() {
        _uiState.update { it.copy(showFavorites = true) }
        loadFavorites()
    }

    fun exitFavoritesMode() {
        _uiState.update { it.copy(showFavorites = false) }
        loadFiles()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            favoriteRepository.getAllFavorites()
                .catch { e ->
                    Logger.e("HomeViewModel", "Failed to load favorites", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { favorites ->
                    _uiState.update { it.copy(isLoading = false, files = favorites) }
                }
        }
    }

    fun enterRecentMode() {
        _uiState.update { it.copy(showRecent = true) }
        loadRecentFiles()
    }

    fun exitRecentMode() {
        _uiState.update { it.copy(showRecent = false) }
        loadFiles()
    }

    private fun loadRecentFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            recentRepository.getRecentFiles()
                .catch { e ->
                    Logger.e("HomeViewModel", "Failed to load recent files", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { recentFiles ->
                    _uiState.update { it.copy(isLoading = false, files = recentFiles) }
                }
        }
    }

    fun addToRecent(file: FileItem) {
        viewModelScope.launch {
            recentRepository.addRecent(file)
        }
    }

    fun shareFile(file: FileItem) {
        _uiState.update { it.copy(shareFilePath = file.path) }
    }

    fun clearShareFile() {
        _uiState.update { it.copy(shareFilePath = null) }
    }
}