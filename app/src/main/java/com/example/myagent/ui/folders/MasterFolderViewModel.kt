package com.example.myagent.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Photo
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MasterFolderViewModel @Inject constructor(
    private val repository: MasterFolderRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    private val _allFolders = MutableStateFlow<List<MasterFolder>>(emptyList())
    val allFolders: StateFlow<List<MasterFolder>> = _allFolders.asStateFlow()
    val filteredFolders: StateFlow<List<MasterFolder>> =
        combine(_allFolders, searchQuery) { folders, query ->
            if (query.isBlank()) folders else folders.filter { it.name.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    private val _previewsByFolder = MutableStateFlow<Map<String, List<Photo>>>(emptyMap())
    val previewsByFolder: StateFlow<Map<String, List<Photo>>> = _previewsByFolder.asStateFlow()

    init {
        viewModelScope.launch {
            val source: Flow<List<MasterFolder>> = repository.getAll()
            source.collect { list -> _allFolders.value = list }
        }
        viewModelScope.launch {
            photoRepository.getAll().collect { photos ->
                _previewsByFolder.value = photos
                    .mapNotNull { photo -> photo.folderUuid?.let { uuid -> uuid to photo } }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, list) -> list.sortedByDescending { it.createdAt }.take(PREVIEW_LIMIT) }
            }
        }
    }

    fun createFolder(name: String) {
        val folder = MasterFolder(
            uuid = UUID.randomUUID().toString(),
            name = name.trim(),
            type = "object",
            createdAt = System.currentTimeMillis(),
            lat = null,
            lon = null
        )
        viewModelScope.launch { repository.insert(folder) }
    }

    fun deleteFolder(folder: MasterFolder) {
        viewModelScope.launch { repository.delete(folder) }
    }

    fun renameFolder(folder: MasterFolder, name: String) {
        viewModelScope.launch { repository.rename(folder.uuid, name.trim()) }
    }

    companion object {
        const val PREVIEW_LIMIT = 4
    }
}