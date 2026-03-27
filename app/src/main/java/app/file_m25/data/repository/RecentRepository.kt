package app.file_m25.data.repository

import app.file_m25.data.local.dao.RecentDao
import app.file_m25.data.local.entity.RecentEntity
import app.file_m25.domain.model.FileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentRepository @Inject constructor(
    private val recentDao: RecentDao
) {
    fun getRecentFiles(): Flow<List<FileItem>> {
        return recentDao.getRecentFiles().map { entities ->
            entities.map { it.toFileItem() }
        }
    }

    suspend fun addRecent(fileItem: FileItem) {
        recentDao.insertRecent(fileItem.toEntity())
        // Keep only last 100 entries
        recentDao.deleteOldRecent(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
    }

    suspend fun removeRecent(path: String) {
        recentDao.deleteRecentByPath(path)
    }

    suspend fun clearAllRecent() {
        recentDao.clearAllRecent()
    }

    fun getRecentCount(): Flow<Int> {
        return recentDao.getRecentCount()
    }

    private fun RecentEntity.toFileItem(): FileItem {
        return FileItem(
            path = path,
            name = name,
            isDirectory = isDirectory,
            size = size,
            lastModified = lastModified,
            extension = if (isDirectory) "" else path.substringAfterLast('.', ""),
            mimeType = ""
        )
    }

    private fun FileItem.toEntity(): RecentEntity {
        return RecentEntity(
            path = path,
            name = name,
            isDirectory = isDirectory,
            size = size,
            lastModified = lastModified
        )
    }
}