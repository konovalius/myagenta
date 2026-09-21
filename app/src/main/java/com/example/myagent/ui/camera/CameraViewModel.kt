package com.example.myagent.ui.camera

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.myagent.data.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _lastPhotoUri = MutableStateFlow<Uri?>(null)
    val lastPhotoUri: StateFlow<Uri?> = _lastPhotoUri.asStateFlow()

    fun capturePhoto(imageCapture: ImageCapture, context: Context) {
        val captureTime = LocalDateTime.now()
        val contentValues = fileRepository.newPhotoContentValues(captureTime)
        val pendingName = contentValues.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            fileRepository.photosCollection(),
            contentValues
        ).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (savedUri != null) {
                        fileRepository.writeDateExif(savedUri, captureTime)
                        fileRepository.setPending(savedUri, false)
                        _lastPhotoUri.value = savedUri
                        Toast.makeText(context, "Фото сохранено в галерею", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    pendingName?.let { fileRepository.deletePendingPhoto(it) }
                    Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )
    }

    fun deleteLastPhoto(): Boolean {
        val uri = _lastPhotoUri.value ?: return false
        val deleted = fileRepository.delete(uri)
        if (deleted) {
            _lastPhotoUri.value = null
        }
        return deleted
    }
}