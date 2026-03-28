package app.file_m25.data.repository

import app.file_m25.data.local.dao.TrashDao
import app.file_m25.data.local.entity.TrashEntity
import app.file_m25.domain.model.FileItem
import app.file_m25.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrashRepository @Inject constructor(
    private val trashDao: TrashDao
) {
    fun getAllTrashItems(): Flow<List<FileItem>> {
        return trashDao.getAllTrashItems().map { entities ->
            entities.map { entity ->
                FileItem(
                    path = entity.originalPath,
                    name = entity.fileName,
                    isDirectory = entity.isDirectory,
                    size = entity.size,
                    mimeType = entity.mimeType,
                    lastModified = entity.deletedTime
                )
            }
        }
    }

    suspend fun addToTrash(file: FileItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(file.path)
            val trashFile = File(getTrashDir(), file.path.hashCode().toString())

            // 确保回收站目录存在
            getTrashDir().mkdirs()

            // 如果是目录，复制整个目录
            if (sourceFile.isDirectory) {
                sourceFile.copyRecursively(trashFile, overwrite = false)
            } else {
                // 确保父目录存在
                trashFile.parentFile?.mkdirs()
                sourceFile.copyTo(trashFile, overwrite = false)
            }

            // 删除原文件
            if (sourceFile.isDirectory) {
                sourceFile.deleteRecursively()
            } else {
                sourceFile.delete()
            }

            // 保存记录到数据库
            val entity = TrashEntity(
                originalPath = file.path,
                fileName = file.name,
                isDirectory = file.isDirectory,
                size = file.size,
                mimeType = file.mimeType,
                deletedTime = System.currentTimeMillis(),
                originalParentPath = sourceFile.parent ?: ""
            )
            trashDao.insert(entity)

            Logger.i("TrashRepository", "Added to trash: ${file.path}")
            true
        } catch (e: Exception) {
            Logger.e("TrashRepository", "Failed to add to trash: ${file.path}", e)
            false
        }
    }

    suspend fun restoreFromTrash(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = trashDao.getTrashItem(path) ?: return@withContext false
            val targetFile = File(entity.originalPath)
            val trashFile = File(getTrashDir(), entity.originalPath.hashCode().toString())

            if (!trashFile.exists()) {
                Logger.e("TrashRepository", "Trash file not found: ${trashFile.absolutePath}")
                return@withContext false
            }

            // 确保目标目录存在
            targetFile.parentFile?.mkdirs()

            // 从回收站复制回原位置
            if (entity.isDirectory) {
                trashFile.copyRecursively(targetFile, overwrite = true)
            } else {
                trashFile.copyTo(targetFile, overwrite = true)
            }

            // 删除回收站中的文件
            if (entity.isDirectory) {
                trashFile.deleteRecursively()
            } else {
                trashFile.delete()
            }

            // 从数据库删除记录
            trashDao.delete(entity)

            Logger.i("TrashRepository", "Restored from trash: ${entity.originalPath}")
            true
        } catch (e: Exception) {
            Logger.e("TrashRepository", "Failed to restore from trash: $path", e)
            false
        }
    }

    suspend fun deleteFromTrash(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = trashDao.getTrashItem(path) ?: return@withContext false
            val trashFile = File(getTrashDir(), entity.originalPath.hashCode().toString())

            if (trashFile.exists()) {
                if (entity.isDirectory) {
                    trashFile.deleteRecursively()
                } else {
                    trashFile.delete()
                }
            }

            trashDao.delete(entity)
            Logger.i("TrashRepository", "Permanently deleted from trash: $path")
            true
        } catch (e: Exception) {
            Logger.e("TrashRepository", "Failed to delete from trash: $path", e)
            false
        }
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        try {
            val trashDir = getTrashDir()
            trashDir.listFiles()?.forEach {
                if (it.isDirectory) it.deleteRecursively() else it.delete()
            }
            trashDao.deleteAll()
            Logger.i("TrashRepository", "Emptied trash")
        } catch (e: Exception) {
            Logger.e("TrashRepository", "Failed to empty trash", e)
        }
    }

    suspend fun getTrashCount(): Int {
        return trashDao.getTrashCount()
    }

    private fun getTrashDir(): File {
        val externalDir = android.os.Environment.getExternalStorageDirectory()
        return File(externalDir, ".trash")
    }
}