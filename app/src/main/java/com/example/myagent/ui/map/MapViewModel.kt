package com.example.myagent.ui.map

import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.repository.FileRepository
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.PhotoRepository
import com.example.myagent.data.util.PlaceNameResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class GeoFolderPin(
    val folder: MasterFolder,
    val photoUri: String?
)

data class GeoPickFolder(
    val uuid: String,
    val name: String,
    val distanceMeters: Float
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val masterFolderRepository: MasterFolderRepository,
    private val photoRepository: PhotoRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _geoFolders = MutableStateFlow<List<GeoFolderPin>>(emptyList())
    val geoFolders: StateFlow<List<GeoFolderPin>> = _geoFolders.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val folders = masterFolderRepository.getAll().first()
            val geo = folders.filter { it.type == "geo" && it.lat != null && it.lon != null }
            _geoFolders.value = geo.map { folder ->
                GeoFolderPin(
                    folder = folder,
                    photoUri = photoRepository.getLastPhotoInFolder(folder.uuid)?.uri
                )
            }
        }
    }

    suspend fun findGeoFoldersNear(
        lat: Double,
        lon: Double,
        maxDistanceMeters: Float = 20f
    ): List<GeoPickFolder> {
        val folders = masterFolderRepository.getAll().first()
        val result = mutableListOf<GeoPickFolder>()
        folders.forEach { folder ->
            val folderLat = folder.lat ?: return@forEach
            val folderLon = folder.lon ?: return@forEach
            if (folder.type != "geo") return@forEach
            val distance = FloatArray(1)
            Location.distanceBetween(lat, lon, folderLat, folderLon, distance)
            if (distance[0] <= maxDistanceMeters) {
                result += GeoPickFolder(
                    uuid = folder.uuid,
                    name = folder.name,
                    distanceMeters = distance[0]
                )
            }
        }
        return result
    }

    suspend fun createGeoFolder(lat: Double, lon: Double): MasterFolder {
        val folderName = PlaceNameResolver.resolve(lat, lon)
        Log.wtf("MapVM", "createGeoFolder($lat,$lon): имя папки '$folderName'")
        val folder = MasterFolder(
            uuid = UUID.randomUUID().toString(),
            name = folderName,
            type = "geo",
            createdAt = System.currentTimeMillis(),
            lat = lat,
            lon = lon
        )
        masterFolderRepository.insert(folder)
        return folder
    }

    suspend fun deleteFolderWithPhotos(folder: MasterFolder) {
        val photos = photoRepository.getByFolder(folder.uuid).first()
        photos.forEach { photo ->
            fileRepository.delete(Uri.parse(photo.uri))
            photoRepository.delete(photo)
        }
        masterFolderRepository.delete(folder)
        refresh()
    }
}