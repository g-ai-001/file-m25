package app.file_m25.di

import android.content.Context
import androidx.room.Room
import app.file_m25.data.local.AppDatabase
import app.file_m25.data.local.dao.FavoriteDao
import app.file_m25.data.local.dao.RecentDao
import app.file_m25.data.local.dao.TrashDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "file_manager_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    @Singleton
    fun provideRecentDao(database: AppDatabase): RecentDao {
        return database.recentDao()
    }

    @Provides
    @Singleton
    fun provideTrashDao(database: AppDatabase): TrashDao {
        return database.trashDao()
    }
}