package com.example.myagent.ui.folders

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.myagent.data.repository.FileRepository
import com.example.myagent.data.repository.MasterFolderRepository
import com.example.myagent.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val fileRepository: FileRepository,
    private val masterFolderRepository: MasterFolderRepository
) : ViewModel() {

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

    private suspend fun deleteFolderIfEmpty(folderUuid: String) {
        if (photoRepository.getByFolder(folderUuid).first().isEmpty()) {
            masterFolderRepository.getAll().first()
                .firstOrNull { it.uuid == folderUuid }
                ?.let { masterFolderRepository.delete(it) }
        }
    }
}