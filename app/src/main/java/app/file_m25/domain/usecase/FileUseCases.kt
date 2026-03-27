package app.file_m25.domain.usecase

import app.file_m25.domain.model.FileItem
import app.file_m25.domain.model.SortMode
import app.file_m25.domain.repository.FileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFilesUseCase @Inject constructor(
    private val repository: FileRepository
) {
    operator fun invoke(path: String, sortMode: SortMode = SortMode.NAME_ASC): Flow<List<FileItem>> {
        return repository.getFiles(path).map { files ->
            sortFiles(files, sortMode)
        }
    }

    private fun sortFiles(files: List<FileItem>, sortMode: SortMode): List<FileItem> {
        val directories = files.filter { it.isDirectory }
        val regularFiles = files.filter { !it.isDirectory }

        val sortedDirs = when (sortMode) {
            SortMode.NAME_ASC -> directories.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> directories.sortedByDescending { it.name.lowercase() }
            SortMode.SIZE_ASC -> directories.sortedBy { it.size }
            SortMode.SIZE_DESC -> directories.sortedByDescending { it.size }
            SortMode.DATE_ASC -> directories.sortedBy { it.lastModified }
            SortMode.DATE_DESC -> directories.sortedByDescending { it.lastModified }
        }

        val sortedFiles = when (sortMode) {
            SortMode.NAME_ASC -> regularFiles.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> regularFiles.sortedByDescending { it.name.lowercase() }
            SortMode.SIZE_ASC -> regularFiles.sortedBy { it.size }
            SortMode.SIZE_DESC -> regularFiles.sortedByDescending { it.size }
            SortMode.DATE_ASC -> regularFiles.sortedBy { it.lastModified }
            SortMode.DATE_DESC -> regularFiles.sortedByDescending { it.lastModified }
        }

        return sortedDirs + sortedFiles
    }
}

class SearchFilesUseCase @Inject constructor(
    private val repository: FileRepository
) {
    operator fun invoke(query: String, searchPath: String): Flow<List<FileItem>> {
        return repository.searchFiles(query, searchPath)
    }
}

class CreateFolderUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(path: String, name: String): Result<Unit> {
        return repository.createFolder(path, name)
    }
}

class DeleteFileUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(path: String): Result<Unit> {
        return repository.deleteFile(path)
    }
}

class RenameFileUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(oldPath: String, newName: String): Result<String> {
        return repository.renameFile(oldPath, newName)
    }
}

class CopyFileUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(sourcePath: String, destFolder: String): Result<String> {
        return repository.copyFile(sourcePath, destFolder)
    }
}

class MoveFileUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(sourcePath: String, destFolder: String): Result<String> {
        return repository.moveFile(sourcePath, destFolder)
    }
}

class GetStorageInfoUseCase @Inject constructor(
    private val repository: FileRepository
) {
    suspend operator fun invoke(): StorageInfo {
        return repository.getStorageInfo()
    }
}