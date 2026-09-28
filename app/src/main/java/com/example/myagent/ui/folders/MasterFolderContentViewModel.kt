package com.example.myagent.ui.folders

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Photo
import com.example.myagent.data.repository.FileRepository
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class MasterFolderContentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    private val masterFolderRepository: MasterFolderRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val folderUuid: String = checkNotNull(savedStateHandle["folderUuid"])

    private val _folder = MutableStateFlow<MasterFolder?>(null)
    val folder: StateFlow<MasterFolder?> = _folder.asStateFlow()

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    init {
        viewModelScope.launch {
            masterFolderRepository.getAll().collect { folders ->
                _folder.value = folders.find { it.uuid == folderUuid }
            }
        }
        viewModelScope.launch {
            photoRepository.getByFolder(folderUuid).collect { list ->
                _photos.value = list
            }
        }
    }

    fun renameFolder(folder: MasterFolder, name: String) {
        viewModelScope.launch { masterFolderRepository.rename(folder.uuid, name.trim()) }
    }

    fun deleteFolderWithPhotos(folder: MasterFolder) {
        viewModelScope.launch {
            photoRepository.getByFolder(folder.uuid).first().forEach { photo ->
                fileRepository.delete(Uri.parse(photo.uri))
                photoRepository.delete(photo)
            }
            masterFolderRepository.delete(folder)
        }
    }
}
