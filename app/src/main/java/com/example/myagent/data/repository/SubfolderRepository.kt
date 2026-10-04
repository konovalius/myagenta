package com.example.myagent.data.repository

import com.example.myagent.data.db.dao.SubfolderDao
import com.example.myagent.data.db.entity.Subfolder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SubfolderRepository @Inject constructor(
    private val subfolderDao: SubfolderDao
) {
    fun getByFolder(folderUuid: String): Flow<List<Subfolder>> = subfolderDao.getByFolder(folderUuid)

    fun getUnassigned(): Flow<List<Subfolder>> = subfolderDao.getUnassigned()

    suspend fun getByAnchor(anchorMediaUuid: String): Subfolder? = subfolderDao.getByAnchor(anchorMediaUuid)

    suspend fun getById(uuid: String): Subfolder? = subfolderDao.getById(uuid)

    suspend fun insert(subfolder: Subfolder) = subfolderDao.insert(subfolder)

    suspend fun delete(subfolder: Subfolder) = subfolderDao.delete(subfolder)

    suspend fun deleteIfEmpty(uuid: String): Int = subfolderDao.deleteIfEmpty(uuid)
}