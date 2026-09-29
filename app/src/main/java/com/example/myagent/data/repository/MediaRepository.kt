package com.example.myagent.data.repository

import com.example.myagent.data.db.dao.MediaDao
import com.example.myagent.data.db.entity.Media
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class MediaRepository @Inject constructor(
    private val mediaDao: MediaDao
) {
    fun getAll(): Flow<List<Media>> = mediaDao.getAll()

    fun getByFolder(folderUuid: String): Flow<List<Media>> = mediaDao.getByFolder(folderUuid)

    suspend fun getLastMediaInFolder(folderUuid: String): Media? =
        mediaDao.getLastMediaInFolder(folderUuid)

    suspend fun insert(media: Media) = mediaDao.insert(media)

    suspend fun delete(media: Media) = mediaDao.delete(media)

    suspend fun updateFolder(mediaUuid: String, folderUuid: String?) =
        mediaDao.updateFolder(mediaUuid, folderUuid)
}