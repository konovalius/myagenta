package com.example.myagent.ui.camera

import android.content.Context
import android.location.Location
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Photo
import com.example.myagent.data.repository.FileRepository
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val photoRepository: PhotoRepository,
    private val masterFolderRepository: MasterFolderRepository
) : ViewModel() {

    private val _lastPhotoUri = MutableStateFlow<Uri?>(null)
    val lastPhotoUri: StateFlow<Uri?> = _lastPhotoUri.asStateFlow()

    private val _savedPhotoEvent = MutableStateFlow<SavedPhotoEvent?>(null)
    val savedPhotoEvent: StateFlow<SavedPhotoEvent?> = _savedPhotoEvent.asStateFlow()

    var lat: Double? = null
        private set
    var lon: Double? = null
        private set

    var folderUuid: String? = null
        private set

    init {
        Log.wtf("CameraVM", "ViewModel initialized")
    }

    fun setLocation(lat: Double?, lon: Double?) {
        this.lat = lat
        this.lon = lon
        Log.wtf("CameraVM", "lat=$lat, lon=$lon")
    }

    fun setFolderUuid(folderUuid: String?) {
        this.folderUuid = folderUuid
        Log.wtf("CameraVM", "folderUuid=$folderUuid")
    }

    fun capturePhoto(imageCapture: ImageCapture, context: Context) {
        val captureTime = LocalDateTime.now()
        val captureLat = lat
        val captureLon = lon
        val captureFolderUuid = folderUuid
        Log.wtf("CameraVM", "captureLat=$captureLat, captureLon=$captureLon, captureFolderUuid=$captureFolderUuid")
        val contentValues = fileRepository.newPhotoContentValues(captureTime)
        val pendingName = contentValues.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            fileRepository.photosCollection(),
            contentValues
        ).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (savedUri != null) {
                        fileRepository.writeDateExif(savedUri, captureTime)
                        fileRepository.setPending(savedUri, false)
                        _lastPhotoUri.value = savedUri
                        if (captureFolderUuid != null || (captureLat != null && captureLon != null)) {
                            val savedUriString = savedUri.toString()
                            viewModelScope.launch(Dispatchers.IO) {
                                val targetFolderUuid = captureFolderUuid
                                    ?: findOrCreateGeoFolder(captureLat!!, captureLon!!)
                                Log.wtf("CameraVM", "folderUuid=$targetFolderUuid (привязка)")
                                val photo = Photo(
                                    uuid = UUID.randomUUID().toString(),
                                    uri = savedUriString,
                                    folderUuid = targetFolderUuid,
                                    createdAt = System.currentTimeMillis(),
                                    lat = captureLat,
                                    lon = captureLon
                                )
                                photoRepository.insert(photo)
                                Log.wtf("CameraVM", "Photo record created: ${photo.uuid} in folder $targetFolderUuid at $captureLat,$captureLon")
                                if (captureFolderUuid != null) {
                                    _savedPhotoEvent.value = SavedPhotoEvent(savedUri, captureFolderUuid)
                                }
                            }
                        }
                        Toast.makeText(context, "Фото сохранено в галерею", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    pendingName?.let { fileRepository.deletePendingPhoto(it) }
                    Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )
    }

    fun consumeSavedPhotoEvent() {
        _savedPhotoEvent.value = null
    }

    fun deleteLastPhoto(): Boolean {
        val uri = _lastPhotoUri.value ?: return false
        val deleted = fileRepository.delete(uri)
        if (deleted) {
            _lastPhotoUri.value = null
        }
        return deleted
    }

    private suspend fun findOrCreateGeoFolder(lat: Double, lon: Double): String {
        Log.wtf("CameraVM", "findOrCreateGeoFolder(рез=$lat,$lon): начинаю поиск")
        val folders = masterFolderRepository.getAll().first()
        folders.forEach { folder ->
            val folderLat = folder.lat
            val folderLon = folder.lon
            if (folderLat != null && folderLon != null) {
                val results = FloatArray(1)
                Location.distanceBetween(lat, lon, folderLat, folderLon, results)
                if (results[0] <= GEO_RADIUS_METERS) {
                    Log.wtf("CameraVM", "findOrCreateGeoFolder: найдена ${folder.uuid} dist=${results[0]}м")
                    return folder.uuid
                }
            }
        }
        Log.wtf("CameraVM", "findOrCreateGeoFolder: подходящей нет (папок всего ${folders.size}), создаю новую")
        val folder = MasterFolder(
            uuid = UUID.randomUUID().toString(),
            name = String.format(Locale.ROOT, "%.4f, %.4f", lat, lon),
            type = "geo",
            createdAt = System.currentTimeMillis(),
            lat = lat,
            lon = lon
        )
        masterFolderRepository.insert(folder)
        Log.wtf("CameraVM", "findOrCreateGeoFolder: создана ${folder.uuid} name=${folder.name}")
        return folder.uuid
    }

    companion object {
        private const val GEO_RADIUS_METERS = 20f
    }
}

data class SavedPhotoEvent(val uri: Uri, val folderUuid: String)