package app.file_m25.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey
    val originalPath: String,
    val fileName: String,
    val isDirectory: Boolean,
    val size: Long,
    val mimeType: String,
    val deletedTime: Long,
    val originalParentPath: String
)