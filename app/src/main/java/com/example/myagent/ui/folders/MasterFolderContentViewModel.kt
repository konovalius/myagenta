package com.example.myagent.ui.folders

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.db.entity.Subfolder
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
class MasterFolderContentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val masterFolderRepository: MasterFolderRepository,
    private val fileRepository: FileRepository,
    private val subfolderRepository: SubfolderRepository
) : ViewModel() {

    private val folderUuid: String = checkNotNull(savedStateHandle["folderUuid"])

    private val _folder = MutableStateFlow<MasterFolder?>(null)
    val folder: StateFlow<MasterFolder?> = _folder.asStateFlow()

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val media: StateFlow<List<Media>> = _media.asStateFlow()

    private val _subfolders = MutableStateFlow<List<Subfolder>>(emptyList())
    val subfolders: StateFlow<List<Subfolder>> = _subfolders.asStateFlow()

    init {
        viewModelScope.launch {
            masterFolderRepository.getAll().collect { folders ->
                _folder.value = folders.find { it.uuid == folderUuid }
            }
        }
        viewModelScope.launch {
            mediaRepository.getByFolder(folderUuid).collect { list ->
                _media.value = list
            }
        }
        viewModelScope.launch {
            subfolderRepository.getByFolder(folderUuid).collect { list ->
                _subfolders.value = list
            }
        }
    }

    fun renameFolder(folder: MasterFolder, name: String) {
        viewModelScope.launch { masterFolderRepository.rename(folder.uuid, name.trim()) }
    }

    fun deleteMedia(media: Media) {
        viewModelScope.launch {
            fileRepository.delete(Uri.parse(media.uri))
            val subfolderUuid = media.subfolderUuid
            mediaRepository.delete(media)
            subfolderUuid?.let { subfolderRepository.deleteIfEmpty(it) }
        }
    }

    fun deleteFolderWithMedia(folder: MasterFolder) {
        viewModelScope.launch {
            mediaRepository.getByFolder(folder.uuid).first().forEach { media ->
                fileRepository.delete(Uri.parse(media.uri))
                mediaRepository.delete(media)
            }
            masterFolderRepository.delete(folder)
        }
    }
}
