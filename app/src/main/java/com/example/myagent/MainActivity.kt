package com.example.myagent

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.myagent.ui.theme.MyAgentTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyAgentTheme {
                CameraScreen()
            }
        }
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    var allPermissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        allPermissionsGranted = requiredPermissions.all { result[it] == true }
    }

    LaunchedEffect(Unit) {
        if (!allPermissionsGranted) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    var isFrontCamera by remember { mutableStateOf(false) }
    var hasFrontCamera by remember { mutableStateOf(true) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var viewerUri by remember { mutableStateOf<Uri?>(null) }
    var referencePhotoUri by remember { mutableStateOf<Uri?>(null) }

    val pickReferenceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            referencePhotoUri = uri
        }
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                hasFrontCamera =
                    cameraProviderFuture.get().hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    val capturePhoto = {
        val capture = imageCapture
        if (capture == null) {
            Toast.makeText(context, "Камера ещё не готова", Toast.LENGTH_SHORT).show()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val outputOptions = createMediaStoreOutputOptions(context)
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri
                        if (savedUri != null) {
                            lastPhotoUri = savedUri
                            Toast.makeText(context, "Фото сохранено в галерею", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            )
        } else {
            val tempFile = File(context.cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = saveFileToGallery(context, tempFile)
                        if (savedUri != null) {
                            lastPhotoUri = savedUri
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT)
                            .show()
                        tempFile.delete()
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val viewerPhotoUri = viewerUri
        if (viewerPhotoUri != null) {
            PhotoViewerScreen(
                uri = viewerPhotoUri,
                onBack = { viewerUri = null },
                onDelete = {
                    try {
                        val deleted =
                            context.contentResolver.delete(viewerPhotoUri, null, null)
                        if (deleted > 0) {
                            Toast.makeText(context, "Фото удалено", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Фото не найдено", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: SecurityException) {
                        Toast.makeText(context, "Не удалось удалить фото", Toast.LENGTH_SHORT).show()
                    }
                    lastPhotoUri = null
                    viewerUri = null
                }
            )
        } else if (allPermissionsGranted) {
            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                    cameraSelector = cameraSelector,
                    onCameraReady = { imageCapture = it }
                )
                referencePhotoUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.8f),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Ориентир",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(0.5f)
                        )
                        IconButton(
                            onClick = { referencePhotoUri = null },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Убрать ориентир",
                                tint = Color.White
                            )
                        }
                    }
                }
                lastPhotoUri?.let { uri ->
                    ThumbnailButton(
                        uri = uri,
                        onClick = { viewerUri = uri },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularIconButton(
                        icon = Icons.Filled.FlipCameraAndroid,
                        contentDescription = "Переключить камеру",
                        enabled = hasFrontCamera,
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier.offset(x = (-68).dp)
                    )
                    ShutterButton(onClick = capturePhoto)
                    CircularIconButton(
                        icon = Icons.Filled.PhotoLibrary,
                        contentDescription = "Выбрать ориентир",
                        enabled = true,
                        onClick = {
                            pickReferenceLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.offset(x = 68.dp)
                    )
                }
            }
        } else {
            Text(
                text = "Доступ к камере не разрешён. Пожалуйста, разрешите доступ к камере в настройках приложения.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        }
    }
}

private fun createMediaStoreOutputOptions(context: Context): ImageCapture.OutputFileOptions {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }
    return ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()
}

private fun saveFileToGallery(context: Context, file: File): Uri? {
    var savedUri: Uri? = null
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        savedUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (savedUri == null) {
            Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT).show()
        } else {
            context.contentResolver.openOutputStream(savedUri)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "Фото сохранено в галерею", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("CameraPreview", "Ошибка при сохранении фото", e)
        Toast.makeText(context, "Ошибка при сохранении фото", Toast.LENGTH_SHORT).show()
        savedUri = null
    } finally {
        file.delete()
    }
    return savedUri
}

@Composable
fun CameraPreview(
    cameraSelector: CameraSelector,
    onCameraReady: (ImageCapture) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    onCameraReady(imageCapture)
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Не удалось открыть камеру", e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ThumbnailButton(
    uri: Uri,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Последнее фото",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PhotoViewerScreen(
    uri: Uri,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Снимок",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Назад", color = Color.White)
        }
        TextButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Удалить", color = Color.White)
        }
    }
}

@Composable
private fun CircularIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) Color.White else Color.Gray
            )
        }
    }
}

@Composable
private fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shutterScale"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}