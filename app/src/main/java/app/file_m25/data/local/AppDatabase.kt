package app.file_m25.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import app.file_m25.data.local.dao.FavoriteDao
import app.file_m25.data.local.entity.FavoriteEntity

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
}