package com.example.myagent.ui.folders

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.repository.FileRepository
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.MediaRepository
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
    private val mediaRepository: MediaRepository,
    private val fileRepository: FileRepository,
    private val masterFolderRepository: MasterFolderRepository
) : ViewModel() {

    private val folderUuid: String? = savedStateHandle["folderUuid"]

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val media: StateFlow<List<Media>> = _media.asStateFlow()

    init {
        folderUuid?.let { uuid ->
            viewModelScope.launch {
                mediaRepository.getByFolder(uuid).collect { list ->
                    _media.value = list
                }
            }
        }
    }

    suspend fun deleteMedia(uri: Uri): Boolean {
        var rowDeleted = false
        mediaRepository.getAll().first()
            .firstOrNull { it.uri == uri.toString() }
            ?.let { media ->
                val folderUuid = media.folderUuid
                mediaRepository.delete(media)
                rowDeleted = true
                if (folderUuid != null) {
                    deleteFolderIfEmpty(folderUuid)
                }
            }
        val fileDeleted = fileRepository.delete(uri)
        return rowDeleted || fileDeleted
    }

    suspend fun attachMediaToFolder(uri: Uri, folderUuid: String): Boolean {
        val media = mediaRepository.getAll().first()
            .firstOrNull { it.uri == uri.toString() }
            ?: return false
        if (media.folderUuid != folderUuid) {
            mediaRepository.updateFolder(media.uuid, folderUuid)
        }
        return true
    }

    private suspend fun deleteFolderIfEmpty(folderUuid: String) {
        if (mediaRepository.getByFolder(folderUuid).first().isEmpty()) {
            masterFolderRepository.getAll().first()
                .firstOrNull { it.uuid == folderUuid }
                ?.let { masterFolderRepository.delete(it) }
        }
    }
}