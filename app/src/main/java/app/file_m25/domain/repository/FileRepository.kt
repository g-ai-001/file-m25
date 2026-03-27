package app.file_m25.domain.repository

import app.file_m25.domain.model.FileItem
import app.file_m25.domain.model.SortMode
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getFiles(path: String): Flow<List<FileItem>>
    fun searchFiles(query: String, searchPath: String): Flow<List<FileItem>>
    suspend fun getFileInfo(path: String): FileItem?
    suspend fun createFolder(path: String, name: String): Result<Unit>
    suspend fun deleteFile(path: String): Result<Unit>
    suspend fun renameFile(oldPath: String, newName: String): Result<String>
    suspend fun copyFile(sourcePath: String, destFolder: String): Result<String>
    suspend fun moveFile(sourcePath: String, destFolder: String): Result<String>
    suspend fun getStorageInfo(): StorageInfo
}

data class StorageInfo(
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long
)