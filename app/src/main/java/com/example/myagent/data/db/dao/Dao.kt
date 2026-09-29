package com.example.myagent.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Media
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterFolderDao {
    @Query("SELECT * FROM master_folders ORDER BY created_at DESC")
    fun getAll(): Flow<List<MasterFolder>>

    @Insert
    suspend fun insert(folder: MasterFolder)

    @Delete
    suspend fun delete(folder: MasterFolder)

    @Query("UPDATE master_folders SET name = :name WHERE uuid = :uuid")
    suspend fun rename(uuid: String, name: String)
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media ORDER BY created_at DESC")
    fun getAll(): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE folder_uuid = :folderUuid ORDER BY created_at DESC")
    fun getByFolder(folderUuid: String): Flow<List<Media>>

    @Query("SELECT * FROM media WHERE folder_uuid = :folderUuid ORDER BY created_at DESC LIMIT 1")
    suspend fun getLastMediaInFolder(folderUuid: String): Media?

    @Insert
    suspend fun insert(media: Media)

    @Delete
    suspend fun delete(media: Media)

    @Query("UPDATE media SET folder_uuid = :folderUuid WHERE uuid = :mediaUuid")
    suspend fun updateFolder(mediaUuid: String, folderUuid: String?)
}