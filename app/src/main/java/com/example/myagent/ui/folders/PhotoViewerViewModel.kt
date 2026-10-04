package com.example.myagent.ui.folders

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.repository.FileRepository
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.MediaRepository
import com.example.myagent.data.repository.SubfolderRepository
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
    private val masterFolderRepository: MasterFolderRepository,
    private val subfolderRepository: SubfolderRepository
) : ViewModel() {

    private val folderUuid: String? = savedStateHandle["folderUuid"]

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val media: StateFlow<List<Media>> = _media.asStateFlow()

    init {
        val uuid = folderUuid
        val flow = if (uuid != null) {
            mediaRepository.getByFolder(uuid)
        } else {
            // Без мастер-папки — свайп по «Не сортированному»
            mediaRepository.getUnassigned()
        }
        viewModelScope.launch {
            flow.collect { list ->
                _media.value = list
            }
        }
    }

    suspend fun deleteMedia(uri: Uri): Boolean {
        var rowDeleted = false
        mediaRepository.getAll().first()
            .firstOrNull { it.uri == uri.toString() }
            ?.let { media ->
                val folderUuid = media.folderUuid
                val subfolderUuid = media.subfolderUuid
                mediaRepository.delete(media)
                rowDeleted = true
                subfolderUuid?.let { subfolderRepository.deleteIfEmpty(it) }
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
                ?.let {
                    subfolderRepository.deleteByFolder(folderUuid)
                    masterFolderRepository.delete(it)
                }
        }
    }
}