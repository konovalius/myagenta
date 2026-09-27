package com.example.myagent.data.repository

import com.example.myagent.data.db.dao.PhotoDao
import com.example.myagent.data.db.entity.Photo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PhotoRepository @Inject constructor(
    private val photoDao: PhotoDao
) {
    fun getAll(): Flow<List<Photo>> = photoDao.getAll()

    fun getByFolder(folderUuid: String): Flow<List<Photo>> = photoDao.getByFolder(folderUuid)

    suspend fun getLastPhotoInFolder(folderUuid: String): Photo? =
        photoDao.getLastPhotoInFolder(folderUuid)

    suspend fun insert(photo: Photo) = photoDao.insert(photo)

    suspend fun delete(photo: Photo) = photoDao.delete(photo)

    suspend fun updateFolder(photoUuid: String, folderUuid: String?) =
        photoDao.updateFolder(photoUuid, folderUuid)
}