package com.example.myagent.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Photo
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterFolderDao {
    @Query("SELECT * FROM master_folders ORDER BY created_at DESC")
    fun getAll(): Flow<List<MasterFolder>>

    @Insert
    suspend fun insert(folder: MasterFolder)

    @Delete
    suspend fun delete(folder: MasterFolder)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY created_at DESC")
    fun getAll(): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE folder_uuid = :folderUuid ORDER BY created_at DESC")
    fun getByFolder(folderUuid: String): Flow<List<Photo>>

    @Query("SELECT * FROM photos WHERE folder_uuid = :folderUuid ORDER BY created_at DESC LIMIT 1")
    suspend fun getLastPhotoInFolder(folderUuid: String): Photo?

    @Insert
    suspend fun insert(photo: Photo)

    @Delete
    suspend fun delete(photo: Photo)

    @Query("UPDATE photos SET folder_uuid = :folderUuid WHERE uuid = :photoUuid")
    suspend fun updateFolder(photoUuid: String, folderUuid: String?)
}