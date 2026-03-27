package app.file_m25.data.repository

import android.os.Environment
import android.os.StatFs
import app.file_m25.domain.model.FileItem
import app.file_m25.domain.repository.FileRepository
import app.file_m25.domain.repository.StorageInfo
import app.file_m25.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor() : FileRepository {

    override fun getFiles(path: String): Flow<List<FileItem>> = flow {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) {
            emit(emptyList())
            return@flow
        }
        val files = directory.listFiles()?.map { FileItem.fromFile(it) } ?: emptyList()
        emit(files)
    }

    override fun searchFiles(query: String, searchPath: String): Flow<List<FileItem>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val results = mutableListOf<FileItem>()
        searchRecursive(File(searchPath), query, results, maxResults = 100)
        emit(results)
    }

    private fun searchRecursive(
        dir: File,
        query: String,
        results: MutableList<FileItem>,
        maxResults: Int
    ) {
        if (results.size >= maxResults) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (results.size >= maxResults) break
            if (file.name.contains(query, ignoreCase = true)) {
                results.add(FileItem.fromFile(file))
            }
            if (file.isDirectory && !file.name.startsWith(".")) {
                searchRecursive(file, query, results, maxResults)
            }
        }
    }

    override suspend fun getFileInfo(path: String): FileItem? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists()) FileItem.fromFile(file) else null
    }

    override suspend fun createFolder(path: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val newFolder = File(path, name)
            if (newFolder.exists()) {
                Result.failure(Exception("文件夹已存在"))
            } else {
                val created = newFolder.mkdirs()
                if (created) {
                    Logger.i("FileRepository", "Created folder: ${newFolder.absolutePath}")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("创建文件夹失败"))
                }
            }
        } catch (e: Exception) {
            Logger.e("FileRepository", "Failed to create folder", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            val deleted = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            if (deleted) {
                Logger.i("FileRepository", "Deleted: $path")
                Result.success(Unit)
            } else {
                Result.failure(Exception("删除失败"))
            }
        } catch (e: Exception) {
            Logger.e("FileRepository", "Failed to delete", e)
            Result.failure(e)
        }
    }

    override suspend fun renameFile(oldPath: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val oldFile = File(oldPath)
            val newFile = File(oldFile.parentFile, newName)
            if (newFile.exists()) {
                Result.failure(Exception("目标文件已存在"))
            } else {
                val success = oldFile.renameTo(newFile)
                if (success) {
                    Logger.i("FileRepository", "Renamed $oldPath to ${newFile.absolutePath}")
                    Result.success(newFile.absolutePath)
                } else {
                    Result.failure(Exception("重命名失败"))
                }
            }
        } catch (e: Exception) {
            Logger.e("FileRepository", "Failed to rename", e)
            Result.failure(e)
        }
    }

    override suspend fun copyFile(sourcePath: String, destFolder: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val dest = File(destFolder, source.name)
            if (dest.exists()) {
                return@withContext Result.failure(Exception("目标文件已存在"))
            }
            source.copyTo(dest, overwrite = false)
            Logger.i("FileRepository", "Copied $sourcePath to ${dest.absolutePath}")
            Result.success(dest.absolutePath)
        } catch (e: Exception) {
            Logger.e("FileRepository", "Failed to copy", e)
            Result.failure(e)
        }
    }

    override suspend fun moveFile(sourcePath: String, destFolder: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val dest = File(destFolder, source.name)
            if (dest.exists()) {
                return@withContext Result.failure(Exception("目标文件已存在"))
            }
            source.copyTo(dest, overwrite = false)
            source.deleteRecursively()
            Logger.i("FileRepository", "Moved $sourcePath to ${dest.absolutePath}")
            Result.success(dest.absolutePath)
        } catch (e: Exception) {
            Logger.e("FileRepository", "Failed to move", e)
            Result.failure(e)
        }
    }

    override suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.absolutePath)
        val totalSpace = stat.blockSizeLong * stat.blockCountLong
        val freeSpace = stat.blockSizeLong * stat.availableBlocksLong
        val usedSpace = totalSpace - freeSpace
        StorageInfo(totalSpace, freeSpace, usedSpace)
    }
}