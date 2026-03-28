package app.file_m25.data.repository

import android.os.Environment
import android.os.StatFs
import app.file_m25.domain.model.FileCategory
import app.file_m25.domain.model.FileItem
import app.file_m25.domain.model.getFileCategory
import app.file_m25.domain.repository.FileRepository
import app.file_m25.domain.repository.StorageInfo
import app.file_m25.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor() : FileRepository {

    override fun getFiles(path: String, showHiddenFiles: Boolean): Flow<List<FileItem>> = flow {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) {
            emit(emptyList())
            return@flow
        }
        var files = directory.listFiles()?.map { FileItem.fromFile(it) } ?: emptyList()
        if (!showHiddenFiles) {
            files = files.filter { !it.name.startsWith(".") }
        }
        emit(files)
    }

    override fun searchFiles(query: String, searchPath: String, showHiddenFiles: Boolean): Flow<List<FileItem>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val results = mutableListOf<FileItem>()
        searchRecursive(File(searchPath), query, results, maxResults = 100, maxDepth = 10, currentDepth = 0, showHiddenFiles)
        emit(results)
    }

    private fun searchRecursive(
        dir: File,
        query: String,
        results: MutableList<FileItem>,
        maxResults: Int,
        maxDepth: Int,
        currentDepth: Int,
        showHiddenFiles: Boolean
    ) {
        if (currentDepth >= maxDepth || results.size >= maxResults) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (results.size >= maxResults) break
            if (!showHiddenFiles && file.name.startsWith(".")) continue
            if (file.name.contains(query, ignoreCase = true)) {
                results.add(FileItem.fromFile(file))
            }
            if (file.isDirectory && (showHiddenFiles || !file.name.startsWith("."))) {
                searchRecursive(file, query, results, maxResults, maxDepth, currentDepth + 1, showHiddenFiles)
            }
        }
    }

    override fun getFilesByCategory(category: FileCategory, rootPath: String, showHiddenFiles: Boolean): Flow<List<FileItem>> = flow {
        val results = mutableListOf<FileItem>()
        scanCategoryRecursive(File(rootPath), category, results, maxDepth = 15, currentDepth = 0, showHiddenFiles)
        emit(results)
    }

    private fun scanCategoryRecursive(
        dir: File,
        category: FileCategory,
        results: MutableList<FileItem>,
        maxDepth: Int,
        currentDepth: Int,
        showHiddenFiles: Boolean
    ) {
        if (currentDepth >= maxDepth) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (!showHiddenFiles && file.name.startsWith(".")) continue
            if (file.isDirectory) {
                scanCategoryRecursive(file, category, results, maxDepth, currentDepth + 1, showHiddenFiles)
            } else {
                val ext = file.extension.lowercase()
                val mimeType = getMimeType(file)
                if (getFileCategory(ext, mimeType) == category) {
                    results.add(FileItem.fromFile(file))
                }
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
            // 先尝试直接重命名（高效且原子）
            val renameSuccess = source.renameTo(dest)
            if (renameSuccess) {
                Logger.i("FileRepository", "Moved $sourcePath to ${dest.absolutePath}")
                return@withContext Result.success(dest.absolutePath)
            }
            // 如果重命名失败（如同分区跨设备），使用复制+删除方式
            source.copyTo(dest, overwrite = false)
            val deleteSuccess = if (source.isDirectory) {
                source.deleteRecursively()
            } else {
                source.delete()
            }
            if (deleteSuccess) {
                Logger.i("FileRepository", "Moved $sourcePath to ${dest.absolutePath}")
                Result.success(dest.absolutePath)
            } else {
                // 复制成功但删除失败，清理目标文件并返回错误
                dest.deleteRecursively()
                Result.failure(Exception("移动文件失败：无法删除源文件"))
            }
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

    override suspend fun getStorageAnalysis(rootPath: String): Map<FileCategory, Long> = withContext(Dispatchers.IO) {
        val analysis = mutableMapOf<FileCategory, Long>()
        FileCategory.entries.forEach { analysis[it] = 0L }
        analyzeDirectoryRecursive(File(rootPath), analysis, maxDepth = 15, currentDepth = 0)
        analysis
    }

    private fun analyzeDirectoryRecursive(
        dir: File,
        analysis: MutableMap<FileCategory, Long>,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth >= maxDepth) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.name.startsWith(".")) continue
            if (file.isDirectory) {
                analyzeDirectoryRecursive(file, analysis, maxDepth, currentDepth + 1)
            } else {
                val ext = file.extension.lowercase()
                val mimeType = getMimeType(file)
                val category = getFileCategory(ext, mimeType)
                analysis[category] = (analysis[category] ?: 0L) + file.length()
            }
        }
    }

    override suspend fun compressToZip(sourcePaths: List<String>, destPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(destPath)
            ZipOutputStream(FileOutputStream(destFile)).use { zos ->
                for (sourcePath in sourcePaths) {
                    val sourceFile = File(sourcePath)
                    if (sourceFile.exists()) {
                        if (sourceFile.isDirectory) {
                            compressDirectory(zos, sourceFile, sourceFile.parentFile.absolutePath)
                        } else {
                            compressFile(zos, sourceFile, "")
                        }
                    }
                }
            }
            Logger.i("FileRepository", "Compressed ${sourcePaths.size} items to $destPath")
            Result.success(destPath)
        } catch (e: Exception) {
            Logger.e("FileRepository", "Failed to compress files", e)
            Result.failure(e)
        }
    }

    private fun compressDirectory(zos: ZipOutputStream, directory: File, basePath: String) {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                compressDirectory(zos, file, basePath)
            } else {
                compressFile(zos, file, directory.parentFile.absolutePath.removePrefix(basePath).removePrefix("/"))
            }
        }
    }

    private fun compressFile(zos: ZipOutputStream, file: File, relativePath: String) {
        FileInputStream(file).use { fis ->
            val entryName = if (relativePath.isNotEmpty()) {
                "$relativePath/${file.name}"
            } else {
                file.name
            }
            zos.putNextEntry(ZipEntry(entryName))
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }

    override suspend fun extractZip(zipPath: String, destFolder: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val zipFile = File(zipPath)
            val destDir = File(destFolder)
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Logger.i("FileRepository", "Extracted $zipPath to $destFolder")
            Result.success(destFolder)
        } catch (e: Exception) {
            Logger.e("FileRepository", "Failed to extract zip", e)
            Result.failure(e)
        }
    }
}