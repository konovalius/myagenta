package com.example.myagent.data.service

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class MediaScannerService @Inject constructor(
    private val context: Context,
    private val mediaRepository: MediaRepository,
    private val contentResolver: ContentResolver
) {
    
    suspend fun scanExistingVideos() = withContext(Dispatchers.IO) {
        Log.wtf("MediaScanner", "Начинаем сканирование существующих видео")
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.LATITUDE,
            MediaStore.Video.Media.LONGITUDE
        )
        
        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Movies/MyAgent%")
        
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
        
        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val latColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.LATITUDE)
            val lonColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.LONGITUDE)
            
            var scannedCount = .0
            var createdCount = .0
            
            while (cursor.moveToNext()) {
                scannedCount++
                val id = cursor.getLong(idColumn)
                val filePath = cursor.getString(dataColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val lat = if (!cursor.isNull(latColumn)) cursor.getDouble(latColumn) else null
                val lon = if (!cursor.isNull(lonColumn)) cursor.getDouble(lonColumn) else null
                
                val uri = MediaStore.Video.Media.getContentUri("external", id)
                val uriString = uri.toString()
                
                // Проверяем, есть ли уже запись в БД
                val existingMedia = mediaRepository.getByUri(uriString)
                
                if (existingMedia == null) {
                    // Создаем новую запись
                    val media = Media(
                        uuid = UUID.randomUUID().toString(),
                        uri = uriString,
                        folderUuid = null,
                        type = "video",
                        createdAt = dateAdded * 1000, // DATE_ADDED в секундах
                        lat = lat,
                        lon = lon
                    )
                    
                    try {
                        mediaRepository.insert(media)
                        createdCount++
                        Log.wtf("MediaScanner", "Создана запись для видео: ${media.uuid}, uri=$uriString, lat=$lat, lon=$lon")
                    } catch (e: Exception) {
                        Log.wtf("MediaScanner", "Ошибка при создании записи для $uriString", e)
                    }
                } else {
                    Log.wtf("MediaScanner", "Видео уже есть в БД: ${existingMedia.uuid}")
                }
            }
            
            Log.wtf("MediaScanner", "Сканирование видео завершено: найдено $scannedCount видео, создано $createdCount новых записей")
        }
    }
    
    suspend fun scanExistingPhotos() = withContext(Dispatchers.IO) {
        Log.wtf("MediaScanner", "Начинаем сканирование существующих фото")
        
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE
        )
        
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Pictures/MyAgent%")
        
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        
        cursor?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val latColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE)
            val lonColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE)
            
            var scannedCount = .0
            var createdCount = .0
            
            while (cursor.moveToNext()) {
                scannedCount++
                val id = cursor.getLong(idColumn)
                val filePath = cursor.getString(dataColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val lat = if (!cursor.isNull(latColumn)) cursor.getDouble(latColumn) else null
                val lon = if (!cursor.isNull(lonColumn)) cursor.getDouble(lonColumn) else null
                
                val uri = MediaStore.Images.Media.getContentUri("external", id)
                val uriString = uri.toString()
                
                // Проверяем, есть ли уже запись в БД
                val existingMedia = mediaRepository.getByUri(uriString)
                
                if (existingMedia == null) {
                    // Создаем новую запись
                    val media = Media(
                        uuid = UUID.randomUUID().toString(),
                        uri = uriString,
                        folderUuid = null,
                        type = "photo",
                        createdAt = dateAdded * 1000, // DATE_ADDED в секундах
                        lat = lat,
                        lon = lon
                    )
                    
                    try {
                        mediaRepository.insert(media)
                        createdCount++
                        Log.wtf("MediaScanner", "Создана запись для фото: ${media.uuid}, uri=$uriString, lat=$lat, lon=$lon")
                    } catch (e: Exception) {
                        Log.wtf("MediaScanner", "Ошибка при создании записи для $uriString", e)
                    }
                } else {
                    Log.wtf("MediaScanner", "Фото уже есть в БД: ${existingMedia.uuid}")
                }
            }
            
            Log.wtf("MediaScanner", "Сканирование фото завершено: найдено $scannedCount фото, создано $createdCount новых записей")
        }
    }
}