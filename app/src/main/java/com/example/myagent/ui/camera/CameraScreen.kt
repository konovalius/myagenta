package com.example.myagent.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import android.util.Rational
import androidx.camera.core.impl.utils.AspectRatioUtil
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage

@Composable
fun CameraScreen(
    initialReferenceUri: Uri? = null,
    initialLat: Double? = null,
    initialLon: Double? = null,
    onNavigateToMasterFolders: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {}
) {
    val viewModel: CameraViewModel = hiltViewModel()
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
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
    val lastPhotoUri by viewModel.lastPhotoUri.collectAsState()
    var viewerUri by remember { mutableStateOf<Uri?>(null) }
    var referencePhotoUri by remember(initialReferenceUri) { mutableStateOf(initialReferenceUri) }
    var overlayAlpha by remember { mutableStateOf(0.5f) }
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }
    var overlayScale by remember { mutableStateOf(1f) }
    var overlayRotation by remember { mutableStateOf(0f) }
    var isEditingOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(initialLat, initialLon) {
        viewModel.setLocation(initialLat, initialLon)
    }

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
        } else {
            viewModel.capturePhoto(capture, context)
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
                    val deleted = viewModel.deleteLastPhoto()
                    if (deleted) {
                        Toast.makeText(context, "Фото удалено", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Фото не найдено", Toast.LENGTH_SHORT).show()
                    }
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
                
                // Топбар сверху
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraTopBar()
                }
                
                // Тулбар над кнопкой съёмки
                CameraToolbar(
                    onNavigateToMasterFolders = onNavigateToMasterFolders,
                    onNavigateToMap = onNavigateToMap,
                    onOpenGallery = {
                        pickReferenceLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onNavigateToOnboarding = onNavigateToOnboarding,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                )
                
                referencePhotoUri?.let { uri ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Ориентир",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(overlayAlpha)
                                .graphicsLayer {
                                    translationX = overlayOffset.x
                                    translationY = overlayOffset.y
                                    scaleX = overlayScale
                                    scaleY = overlayScale
                                    rotationZ = overlayRotation
                                }
                                .then(
                                    if (isEditingOverlay) {
                                        Modifier.pointerInput(isEditingOverlay) {
                                            detectTransformGestures(panZoomLock = true) {
                                                    _,
                                                    pan,
                                                    zoom,
                                                    rotation ->
                                                overlayOffset += pan
                                                overlayScale =
                                                    (overlayScale * zoom).coerceIn(0.1f, 10f)
                                                overlayRotation += rotation
                                            }
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                        IconButton(
                            onClick = { referencePhotoUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Убрать ориентир",
                                tint = Color.White
                            )
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { isEditingOverlay = !isEditingOverlay },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isEditingOverlay) Color.White
                                        else Color.Black.copy(alpha = 0.6f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = if (isEditingOverlay) {
                                        "Выключить редактирование"
                                    } else {
                                        "Редактировать"
                                    },
                                    tint = if (isEditingOverlay) Color.Black else Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    overlayOffset = Offset.Zero
                                    overlayScale = 1f
                                    overlayRotation = 0f
                                    overlayAlpha = 0.5f
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Сбросить ориентир",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
                if (referencePhotoUri != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .width(56.dp)
                            .height(220.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = overlayAlpha,
                            onValueChange = { overlayAlpha = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(-90f)
                        )
                        Icon(
                            imageVector = Icons.Filled.Opacity,
                            contentDescription = "Прозрачность ориентира",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(4.dp)
                                .size(20.dp)
                        )
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

@Composable
fun CameraPreview(
    cameraSelector: CameraSelector,
    onCameraReady: (ImageCapture) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
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
                    
                    // Создаём ViewPort для согласования Preview и ImageCapture
                    val viewPort = previewView.viewPort ?: ViewPort.Builder(
                        Rational(previewView.width, previewView.height),
                        previewView.display.rotation
                    ).build()
                    
                    // Используем UseCaseGroup с ViewPort
                    val useCaseGroup = UseCaseGroup.Builder()
                        .setViewPort(viewPort)
                        .addUseCase(preview)
                        .addUseCase(imageCapture)
                        .build()
                    
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        useCaseGroup
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