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
import com.example.myagent.data.util.ReverseGeocoder
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

    private val _geoPrompt = MutableStateFlow<GeoPrompt?>(null)
    val geoPrompt: StateFlow<GeoPrompt?> = _geoPrompt.asStateFlow()

    var geoPromptShown = false
        private set

    private var locationFromMap = false
    private var deviceLat: Double? = null
    private var deviceLon: Double? = null

    init {
        Log.wtf("CameraVM", "ViewModel initialized")
    }

    fun setLocation(lat: Double?, lon: Double?) {
        this.lat = lat
        this.lon = lon
        locationFromMap = lat != null && lon != null
        Log.wtf("CameraVM", "lat=$lat, lon=$lon, fromMap=$locationFromMap")
    }

    fun setDeviceLocation(lat: Double?, lon: Double?) {
        deviceLat = lat
        deviceLon = lon
        Log.wtf("CameraVM", "deviceLat=$lat, deviceLon=$lon")
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
        val captureFromMap = locationFromMap
        val captureDeviceLat = deviceLat
        val captureDeviceLon = deviceLon
        Log.wtf("CameraVM", "captureLat=$captureLat, captureLon=$captureLon, captureFolderUuid=$captureFolderUuid, fromMap=$captureFromMap")
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

                        val geoPromptEligible = captureFolderUuid == null &&
                            !captureFromMap &&
                            !geoPromptShown &&
                            captureDeviceLat != null &&
                            captureDeviceLon != null

                        if (geoPromptEligible) {
                            geoPromptShown = true
                            val promptLat = captureDeviceLat!!
                            val promptLon = captureDeviceLon!!
                            viewModelScope.launch(Dispatchers.IO) {
                                val existing = findGeoFolderNear(promptLat, promptLon)
                                _geoPrompt.value = if (existing != null) {
                                    GeoPrompt.ExistingFolder(
                                        uri = savedUri,
                                        lat = promptLat,
                                        lon = promptLon,
                                        folderUuid = existing.uuid,
                                        folderName = existing.name
                                    )
                                } else {
                                    GeoPrompt.NewFolder(
                                        uri = savedUri,
                                        lat = promptLat,
                                        lon = promptLon
                                    )
                                }
                                Log.wtf("CameraVM", "geoPrompt показан: ${_geoPrompt.value}")
                            }
                        } else if (captureFolderUuid != null || (captureLat != null && captureLon != null)) {
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

    fun answerGeoPrompt(saveToFolder: Boolean) {
        val prompt = _geoPrompt.value ?: return
        _geoPrompt.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val targetFolderUuid = if (saveToFolder) {
                when (prompt) {
                    is GeoPrompt.ExistingFolder -> prompt.folderUuid
                    is GeoPrompt.NewFolder -> findOrCreateGeoFolder(prompt.lat, prompt.lon)
                }
            } else {
                null
            }
            val photo = Photo(
                uuid = UUID.randomUUID().toString(),
                uri = prompt.uri.toString(),
                folderUuid = targetFolderUuid,
                createdAt = System.currentTimeMillis(),
                lat = prompt.lat,
                lon = prompt.lon
            )
            photoRepository.insert(photo)
            Log.wtf("CameraVM", "GeoPrompt ответ=$saveToFolder, folder=$targetFolderUuid, photo=${photo.uuid}")
        }
    }

    fun deleteLastPhoto(): Boolean {
        val uri = _lastPhotoUri.value ?: return false
        val deleted = fileRepository.delete(uri)
        if (deleted) {
            _lastPhotoUri.value = null
        }
        return deleted
    }

    private suspend fun findGeoFolderNear(lat: Double, lon: Double): MasterFolder? {
        val folders = masterFolderRepository.getAll().first()
        folders.forEach { folder ->
            val folderLat = folder.lat
            val folderLon = folder.lon
            if (folderLat != null && folderLon != null) {
                val results = FloatArray(1)
                Location.distanceBetween(lat, lon, folderLat, folderLon, results)
                if (results[0] <= GEO_RADIUS_METERS) {
                    Log.wtf("CameraVM", "findGeoFolderNear: найдена ${folder.uuid} (${folder.name}) dist=${results[0]}м")
                    return folder
                }
            }
        }
        Log.wtf("CameraVM", "findGeoFolderNear: подходящей папки нет")
        return null
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
        val placeName = ReverseGeocoder.getPlaceName(lat, lon)
        val folderName = placeName ?: String.format(Locale.ROOT, "%.4f, %.4f", lat, lon)
        Log.wtf("CameraVM", "findOrCreateGeoFolder: геокодирование вернуло '$placeName'")
        val folder = MasterFolder(
            uuid = UUID.randomUUID().toString(),
            name = folderName,
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

sealed interface GeoPrompt {
    val uri: Uri
    val lat: Double
    val lon: Double

    data class ExistingFolder(
        override val uri: Uri,
        override val lat: Double,
        override val lon: Double,
        val folderUuid: String,
        val folderName: String
    ) : GeoPrompt

    data class NewFolder(
        override val uri: Uri,
        override val lat: Double,
        override val lon: Double
    ) : GeoPrompt
}