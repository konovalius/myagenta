package com.example.myagent.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.myagent.data.db.entity.MasterFolder
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