package com.example.myagent.ui.folders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.data.db.entity.Photo
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MasterFolderContentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    photoRepository: PhotoRepository,
    masterFolderRepository: MasterFolderRepository
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
}