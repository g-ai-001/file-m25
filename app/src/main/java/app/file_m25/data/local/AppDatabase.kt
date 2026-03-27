package app.file_m25.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import app.file_m25.data.local.dao.FavoriteDao
import app.file_m25.data.local.dao.RecentDao
import app.file_m25.data.local.entity.FavoriteEntity
import app.file_m25.data.local.entity.RecentEntity

@Database(
    entities = [FavoriteEntity::class, RecentEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
}