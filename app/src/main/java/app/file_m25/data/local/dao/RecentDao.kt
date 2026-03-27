package app.file_m25.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.file_m25.data.local.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_files ORDER BY accessedTime DESC LIMIT 100")
    fun getRecentFiles(): Flow<List<RecentEntity>>

    @Query("SELECT * FROM recent_files WHERE path = :path LIMIT 1")
    suspend fun getRecentByPath(path: String): RecentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentEntity)

    @Delete
    suspend fun deleteRecent(recent: RecentEntity)

    @Query("DELETE FROM recent_files WHERE path = :path")
    suspend fun deleteRecentByPath(path: String)

    @Query("DELETE FROM recent_files")
    suspend fun clearAllRecent()

    @Query("SELECT COUNT(*) FROM recent_files")
    fun getRecentCount(): Flow<Int>

    @Query("DELETE FROM recent_files WHERE accessedTime < :timestamp")
    suspend fun deleteOldRecent(timestamp: Long)
}