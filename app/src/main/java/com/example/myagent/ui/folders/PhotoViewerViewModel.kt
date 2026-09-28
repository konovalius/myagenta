package com.example.myagent.ui.folders

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class PhotoViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    private val fileRepository: FileRepository,
    private val masterFolderRepository: MasterFolderRepository
) : ViewModel() {

    private val folderUuid: String? = savedStateHandle["folderUuid"]

    private val _photos = MutableStateFlow<List<Photo>>(emptyList())
    val photos: StateFlow<List<Photo>> = _photos.asStateFlow()

    init {
        folderUuid?.let { uuid ->
            viewModelScope.launch {
                photoRepository.getByFolder(uuid).collect { list ->
                    _photos.value = list
                }
            }
        }
    }

    suspend fun deletePhoto(uri: Uri): Boolean {
        var rowDeleted = false
        photoRepository.getAll().first()
            .firstOrNull { it.uri == uri.toString() }
            ?.let { photo ->
                val folderUuid = photo.folderUuid
                photoRepository.delete(photo)
                rowDeleted = true
                if (folderUuid != null) {
                    deleteFolderIfEmpty(folderUuid)
                }
            }
        val fileDeleted = fileRepository.delete(uri)
        return rowDeleted || fileDeleted
    }

    suspend fun attachPhotoToFolder(uri: Uri, folderUuid: String): Boolean {
        val photo = photoRepository.getAll().first()
            .firstOrNull { it.uri == uri.toString() }
            ?: return false
        if (photo.folderUuid != folderUuid) {
            photoRepository.updateFolder(photo.uuid, folderUuid)
        }
        return true
    }

    private suspend fun deleteFolderIfEmpty(folderUuid: String) {
        if (photoRepository.getByFolder(folderUuid).first().isEmpty()) {
            masterFolderRepository.getAll().first()
                .firstOrNull { it.uuid == folderUuid }
                ?.let { masterFolderRepository.delete(it) }
        }
    }
}