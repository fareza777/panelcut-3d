package com.panelcut3d.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String,
    val widthMm: Float,
    val heightMm: Float,
    val depthMm: Float,
    val panelCount: Int,
    val designJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): ProjectEntity?

    @Insert
    suspend fun insert(entity: ProjectEntity): Long

    @Update
    suspend fun update(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM projects")
    suspend fun deleteAll(): Int
}

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class PanelCutDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        fun build(context: Context): PanelCutDatabase =
            Room.databaseBuilder(context, PanelCutDatabase::class.java, "panelcut.db")
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
