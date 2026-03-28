package app.file_m25.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.file_m25.data.local.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash ORDER BY deletedTime DESC")
    fun getAllTrashItems(): Flow<List<TrashEntity>>

    @Query("SELECT * FROM trash WHERE originalPath = :path LIMIT 1")
    suspend fun getTrashItem(path: String): TrashEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trashEntity: TrashEntity)

    @Delete
    suspend fun delete(trashEntity: TrashEntity)

    @Query("DELETE FROM trash WHERE originalPath = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM trash")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM trash")
    suspend fun getTrashCount(): Int
}