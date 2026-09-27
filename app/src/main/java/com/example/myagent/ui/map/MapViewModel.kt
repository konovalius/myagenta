package com.example.myagent.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class MapViewModel @Inject constructor(
    private val masterFolderRepository: MasterFolderRepository,
    private val photoRepository: PhotoRepository
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
}