package com.example.myagent.ui.map

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PastVu"
private const val ARCHIVE_IMAGE_BASE_URL = "https://img.pastvu.com/d/"
private const val ARCHIVE_ALBUM_PATH = "Pictures/PastVu"
private const val DOWNLOAD_TIMEOUT_MS = 20_000

enum class ArchiveSaveState {
    Idle,
    Saving,
    Saved,
    Error
}

@HiltViewModel
class ArchivePhotoViewerViewModel @Inject constructor(
    private val contentResolver: ContentResolver
) : ViewModel() {

    private val _saveState = MutableStateFlow(ArchiveSaveState.Idle)
    val saveState: StateFlow<ArchiveSaveState> = _saveState.asStateFlow()

    fun saveToGallery(cid: Long, file: String) {
        if (_saveState.value == ArchiveSaveState.Saving) return
        _saveState.value = ArchiveSaveState.Saving
        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = try {
                val bytes = download(ARCHIVE_IMAGE_BASE_URL + file)
                insertIntoGallery(cid, bytes)
                ArchiveSaveState.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.wtf(TAG, "save failed: ${e.javaClass.simpleName}: ${e.message}")
                ArchiveSaveState.Error
            }
        }
    }

    fun consumeSaveState() {
        _saveState.value = ArchiveSaveState.Idle
    }

    private fun download(url: String): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = DOWNLOAD_TIMEOUT_MS
            readTimeout = DOWNLOAD_TIMEOUT_MS
            setRequestProperty("User-Agent", "MyAgentApp/1.0 (Android)")
        }
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("http=$code")
            }
            return connection.inputStream.use { stream -> stream.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun insertIntoGallery(cid: Long, bytes: ByteArray) {
        val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date())
        val displayName = "PastVu_${cid}_$stamp.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, ARCHIVE_ALBUM_PATH)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("MediaStore insert вернул null")
        try {
            contentResolver.openOutputStream(uri)?.use { stream -> stream.write(bytes) }
                ?: throw IllegalStateException("openOutputStream вернул null")
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        contentResolver.update(uri, values, null, null)
        Log.wtf(TAG, "saved: name=$displayName bytes=${bytes.size} uri=$uri")
    }
}