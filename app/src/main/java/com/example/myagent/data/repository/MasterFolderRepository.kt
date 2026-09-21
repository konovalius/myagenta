package com.example.myagent.data.repository

import com.example.myagent.data.db.dao.MasterFolderDao
import com.example.myagent.data.db.entity.MasterFolder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class MasterFolderRepository @Inject constructor(
    private val masterFolderDao: MasterFolderDao
) {
    fun getAll(): Flow<List<MasterFolder>> = masterFolderDao.getAll()

    suspend fun insert(folder: MasterFolder) = masterFolderDao.insert(folder)

    suspend fun delete(folder: MasterFolder) = masterFolderDao.delete(folder)
}