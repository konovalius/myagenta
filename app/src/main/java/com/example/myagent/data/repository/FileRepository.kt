package com.example.myagent.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun storageDir(): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun newMediaFile(extension: String): File {
        return File(storageDir(), "${UUID.randomUUID()}.$extension")
    }

    fun listMedia(): List<File> {
        return storageDir()
            .listFiles { file -> file.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun newPhotoName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        return "$timestamp.jpg"
    }

    fun newPhotoContentValues(): ContentValues {
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newPhotoName())
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MyAgent")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    fun photosCollection(): Uri {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    fun setPending(uri: Uri, pending: Boolean) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, if (pending) 1 else 0)
        }
        context.contentResolver.update(uri, values, null, null)
    }

    fun delete(uri: Uri): Boolean {
        return try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: SecurityException) {
            false
        }
    }

    fun deletePendingPhoto(name: String): Boolean {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.IS_PENDING}=1 AND " +
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?"
        val args = arrayOf(name)
        context.contentResolver.query(
            photosCollection(),
            projection,
            selection,
            args,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                val uri = ContentUris.withAppendedId(photosCollection(), id)
                return try {
                    context.contentResolver.delete(uri, null, null) > 0
                } catch (e: SecurityException) {
                    false
                }
            }
        }
        return false
    }
}