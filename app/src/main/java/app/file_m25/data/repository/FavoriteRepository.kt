package app.file_m25.data.repository

import app.file_m25.data.local.dao.FavoriteDao
import app.file_m25.data.local.entity.FavoriteEntity
import app.file_m25.domain.model.FileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao
) {
    fun getAllFavorites(): Flow<List<FileItem>> {
        return favoriteDao.getAllFavorites().map { entities ->
            entities.map { it.toFileItem() }
        }
    }

    fun isFavorite(path: String): Flow<Boolean> {
        return favoriteDao.isFavorite(path)
    }

    suspend fun addFavorite(fileItem: FileItem) {
        favoriteDao.insertFavorite(fileItem.toEntity())
    }

    suspend fun removeFavorite(path: String) {
        favoriteDao.deleteFavoriteByPath(path)
    }

    fun getFavoriteCount(): Flow<Int> {
        return favoriteDao.getFavoriteCount()
    }

    private fun FavoriteEntity.toFileItem(): FileItem {
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

    private fun FileItem.toEntity(): FavoriteEntity {
        return FavoriteEntity(
            path = path,
            name = name,
            isDirectory = isDirectory,
            size = size,
            lastModified = lastModified
        )
    }
}