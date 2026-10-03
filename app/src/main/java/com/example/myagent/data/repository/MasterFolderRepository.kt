package com.example.myagent.data.repository

import com.example.myagent.data.db.dao.MasterFolderDao
import com.example.myagent.data.db.entity.MasterFolder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
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

    suspend fun rename(uuid: String, name: String) = masterFolderDao.rename(uuid, name)

    suspend fun createDefaultFolder(): MasterFolder {
        val folder = MasterFolder(
            uuid = UUID.randomUUID().toString(),
            name = DEFAULT_NAME_FORMAT.format(LocalDateTime.now()),
            type = "object",
            createdAt = System.currentTimeMillis(),
            lat = null,
            lon = null
        )
        masterFolderDao.insert(folder)
        return folder
    }

    private companion object {
        private val DEFAULT_NAME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("'Папка' dd.MM HH:mm", Locale.getDefault())
    }
}