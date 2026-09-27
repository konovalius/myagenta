package com.example.myagent.ui.folders

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.myagent.data.repository.FileRepository
import com.example.myagent.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    suspend fun deletePhoto(uri: Uri): Boolean {
        var rowDeleted = false
        photoRepository.getAll().first()
            .firstOrNull { it.uri == uri.toString() }
            ?.let {
                photoRepository.delete(it)
                rowDeleted = true
            }
        val fileDeleted = fileRepository.delete(uri)
        return rowDeleted || fileDeleted
    }
}