package com.example.myagent.ui.camera

import android.content.Context
import android.location.Location
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.repository.FileRepository
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.MediaRepository
import com.example.myagent.data.util.PlaceNameResolver
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
    private val mediaRepository: MediaRepository,
    private val masterFolderRepository: MasterFolderRepository
) : ViewModel() {

    private val _lastPhotoUri = MutableStateFlow<Uri?>(null)
    val lastPhotoUri: StateFlow<Uri?> = _lastPhotoUri.asStateFlow()

    private val _savedPhotoEvent = MutableStateFlow<SavedPhotoEvent?>(null)
    val savedPhotoEvent: StateFlow<SavedPhotoEvent?> = _savedPhotoEvent.asStateFlow()

    private var activeRecording: Recording? = null

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
                                val media = Media(
                                    uuid = UUID.randomUUID().toString(),
                                    uri = savedUriString,
                                    folderUuid = targetFolderUuid,
                                    type = "photo",
                                    createdAt = System.currentTimeMillis(),
                                    lat = captureLat,
                                    lon = captureLon
                                )
                                mediaRepository.insert(media)
                                Log.wtf("CameraVM", "Media record created: ${media.uuid} in folder $targetFolderUuid at $captureLat,$captureLon")
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
            val media = Media(
                uuid = UUID.randomUUID().toString(),
                uri = prompt.uri.toString(),
                folderUuid = targetFolderUuid,
                type = "photo",
                createdAt = System.currentTimeMillis(),
                lat = prompt.lat,
                lon = prompt.lon
            )
            mediaRepository.insert(media)
            Log.wtf("CameraVM", "GeoPrompt ответ=$saveToFolder, folder=$targetFolderUuid, media=${media.uuid}")
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

    fun startVideoRecording(videoCapture: VideoCapture<Recorder>, context: Context): Boolean {
        if (activeRecording != null) {
            return false
        }

        val captureTime = LocalDateTime.now()
        val captureLat = lat
        val captureLon = lon
        val captureFolderUuid = folderUuid
        val captureFromMap = locationFromMap
        val captureDeviceLat = deviceLat
        val captureDeviceLon = deviceLon

        val videoFile = fileRepository.newMediaFile("mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        activeRecording = null
                        val outputUri = fileRepository.saveVideoToMediaStore(videoFile, captureTime)
                        if (outputUri != null) {
                            
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
                                            uri = outputUri,
                                            lat = promptLat,
                                            lon = promptLon,
                                            folderUuid = existing.uuid,
                                            folderName = existing.name
                                        )
                                    } else {
                                        GeoPrompt.NewFolder(
                                            uri = outputUri,
                                            lat = promptLat,
                                            lon = promptLon
                                        )
                                    }
                                }
                            } else if (captureFolderUuid != null || (captureLat != null && captureLon != null)) {
                                val outputUriString = outputUri.toString()
                                viewModelScope.launch(Dispatchers.IO) {
                                    val targetFolderUuid = captureFolderUuid
                                        ?: findOrCreateGeoFolder(captureLat!!, captureLon!!)
                                    val media = Media(
                                        uuid = UUID.randomUUID().toString(),
                                        uri = outputUriString,
                                        folderUuid = targetFolderUuid,
                                        type = "video",
                                        createdAt = System.currentTimeMillis(),
                                        lat = captureLat,
                                        lon = captureLon
                                    )
                                    mediaRepository.insert(media)
                                    if (captureFolderUuid != null) {
                                        _savedPhotoEvent.value = SavedPhotoEvent(outputUri, captureFolderUuid)
                                    }
                                }
                            }
                        }
                    }
                }
            }

        return true
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun isRecording(): Boolean = activeRecording != null

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
        val folder = MasterFolder(
            uuid = UUID.randomUUID().toString(),
            name = PlaceNameResolver.formatCoordinates(lat, lon),
            type = "geo",
            createdAt = System.currentTimeMillis(),
            lat = lat,
            lon = lon
        )
        masterFolderRepository.insert(folder)
        Log.wtf("CameraVM", "findOrCreateGeoFolder: создана ${folder.uuid} с координатами, имя уточняется в фоне")
        resolveNameInBackground(folder.uuid, lat, lon)
        return folder.uuid
    }

    private fun resolveNameInBackground(uuid: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            val placeName = PlaceNameResolver.resolve(lat, lon)
            if (placeName.isNotEmpty()) {
                masterFolderRepository.rename(uuid, placeName)
                Log.wtf("CameraVM", "resolveNameInBackground: $uuid переименована в '$placeName'")
            } else {
                Log.wtf("CameraVM", "resolveNameInBackground: $uuid остаётся с координатами")
            }
        }
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