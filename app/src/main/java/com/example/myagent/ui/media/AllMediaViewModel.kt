package com.example.myagent.ui.media

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myagent.data.db.entity.Subfolder
import com.example.myagent.data.db.entity.Media
import com.example.myagent.data.repository.FileRepository
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.MediaRepository
import com.example.myagent.data.repository.SubfolderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AllMediaViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val masterFolderRepository: MasterFolderRepository,
    private val subfolderRepository: SubfolderRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _unassignedMedia = MutableStateFlow<List<Media>>(emptyList())
    val unassignedMedia: StateFlow<List<Media>> = _unassignedMedia.asStateFlow()

    private val _subfolders = MutableStateFlow<List<Subfolder>>(emptyList())
    val subfolders: StateFlow<List<Subfolder>> = _subfolders.asStateFlow()

    init {
        viewModelScope.launch {
            val source: Flow<List<Media>> = mediaRepository.getUnassigned()
            source.collect { list -> _unassignedMedia.value = list }
        }
        viewModelScope.launch {
            val source: Flow<List<Subfolder>> = subfolderRepository.getUnassigned()
            source.collect { list -> _subfolders.value = list }
        }
    }

    fun deleteMedia(media: Media) {
        viewModelScope.launch {
            val subfolderUuid = media.subfolderUuid
            mediaRepository.delete(media)
            subfolderUuid?.let { subfolderRepository.deleteIfEmpty(it) }
        }
    }

    fun deleteSubfolder(subfolder: Subfolder) {
        viewModelScope.launch(Dispatchers.IO) {
            subfolderRepository.delete(subfolder)
        }
    }

    // Удалить подпапку, а медиа вернуть в «Не сортированное»
    fun deleteSubfolderKeepMedia(subfolder: Subfolder) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaList = mediaRepository.getBySubfolderOnce(subfolder.uuid)
            mediaList.forEach { media ->
                mediaRepository.update(media.copy(subfolderUuid = null))
            }
            subfolderRepository.delete(subfolder)
        }
    }

    // Удалить подпапку вместе с медиа (записи + файлы)
    fun deleteSubfolderWithMedia(subfolder: Subfolder) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaList = mediaRepository.getBySubfolderOnce(subfolder.uuid)
            mediaList.forEach { media ->
                fileRepository.delete(Uri.parse(media.uri))
                mediaRepository.delete(media)
            }
            subfolderRepository.delete(subfolder)
        }
    }

    fun defaultFolderName(): String = masterFolderRepository.defaultFolderName()

    fun mergeSelected(selectedIds: List<String>, createMasterFolder: Boolean, masterFolderName: String) {
        if (selectedIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val found = mediaRepository.getByUuids(selectedIds)
                val ordered = selectedIds.mapNotNull { id -> found.firstOrNull { it.uuid == id } }
                if (ordered.isEmpty()) {
                    Log.wtf("Merge", "Нет медиа для объединения")
                    return@launch
                }
                val anchor = ordered.first()
                val targetFolderUuid = if (createMasterFolder) {
                    masterFolderRepository.createFolder(
                        masterFolderName.ifBlank { masterFolderRepository.defaultFolderName() }
                    ).uuid
                } else {
                    null
                }
                val subfolderUuid = UUID.randomUUID().toString()
                subfolderRepository.insert(
                    Subfolder(
                        uuid = subfolderUuid,
                        folderUuid = targetFolderUuid,
                        anchorMediaUuid = anchor.uuid,
                        createdAt = System.currentTimeMillis()
                    )
                )
                ordered.forEach { media ->
                    mediaRepository.update(
                        media.copy(
                            folderUuid = targetFolderUuid ?: media.folderUuid,
                            subfolderUuid = subfolderUuid
                        )
                    )
                }
                Log.wtf(
                    "Merge",
                    "Объединено ${ordered.size}: folderUuid=$targetFolderUuid, subfolderUuid=$subfolderUuid, anchor=${anchor.uuid}"
                )
            } catch (e: Exception) {
                Log.e("Merge", "Ошибка объединения", e)
            }
        }
    }
}