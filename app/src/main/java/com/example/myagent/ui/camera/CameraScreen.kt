package com.example.myagent.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import coil.compose.AsyncImage
import com.example.myagent.ui.common.pressScale

@Composable
fun CameraScreen(
    initialReferenceUri: Uri? = null,
    initialLat: Double? = null,
    initialLon: Double? = null,
    initialFolderUuid: String? = null,
    onNavigateToMasterFolders: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onPhotoCapturedFromFolder: (Uri, String) -> Unit = { _, _ -> }
) {
    val viewModel: CameraViewModel = hiltViewModel()
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
        }
    }

    val videoPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
        }
    }

    val hasVideoPermissions by remember {
        mutableStateOf(
            videoPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
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

    val locationPermissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    var deviceLocation by remember { mutableStateOf<Location?>(null) }
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val cancellationToken = remember { CancellationTokenSource() }

    fun refreshDeviceLocation() {
        val granted = locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!granted) {
            deviceLocation = null
            viewModel.setDeviceLocation(null, null)
            return
        }
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                deviceLocation = location
                viewModel.setDeviceLocation(location?.latitude, location?.longitude)
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshDeviceLocation()
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = locationPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (alreadyGranted) {
            refreshDeviceLocation()
        } else {
            locationPermissionLauncher.launch(locationPermissions)
        }
    }

    var isFrontCamera by remember { mutableStateOf(false) }
    var hasFrontCamera by remember { mutableStateOf(true) }
    var isSlowMotionActive by remember { mutableStateOf(false) }
    var isTimelapseActive by remember { mutableStateOf(false) }
    var isVideoMode by remember { mutableStateOf(false) }
    var centeredModeIndex by remember { mutableIntStateOf(0) }
    var activeMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var isRecording by remember { mutableStateOf(false) }
    val lastPhotoUri by viewModel.lastPhotoUri.collectAsState()
    val savedPhotoEvent by viewModel.savedPhotoEvent.collectAsState()
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

    LaunchedEffect(initialFolderUuid) {
        viewModel.setFolderUuid(initialFolderUuid)
    }

    LaunchedEffect(savedPhotoEvent) {
        val event = savedPhotoEvent
        if (event != null) {
            onPhotoCapturedFromFolder(event.uri, event.folderUuid)
            viewModel.consumeSavedPhotoEvent()
        }
    }

    val geoPrompt by viewModel.geoPrompt.collectAsState()

    geoPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = { viewModel.answerGeoPrompt(false) },
            title = { Text("Записать в папку?") },
            text = {
                Text(
                    when (prompt) {
                        is GeoPrompt.ExistingFolder ->
                            "Записать в существующую папку «${prompt.folderName}»?"
                        is GeoPrompt.NewFolder ->
                            "Записать в новую папку в этой точке?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.answerGeoPrompt(true) }) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.answerGeoPrompt(false) }) {
                    Text("Нет")
                }
            }
        )
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

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = videoPermissions.all { result[it] == true }
        if (granted) {
            // Повторяем попытку записи после получения разрешений
            val capture = videoCapture
            if (capture != null) {
                val started = viewModel.startVideoRecording(capture, context)
                if (started) {
                    isRecording = true
                    Toast.makeText(context, "Начата запись видео", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Для записи видео требуются разрешения камеры и микрофона", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleVideoRecording = {
        if (isRecording) {
            viewModel.stopVideoRecording()
            isRecording = false
            Toast.makeText(context, "Запись видео остановлена", Toast.LENGTH_SHORT).show()
        } else {
            val capture = videoCapture
            if (capture == null) {
                Toast.makeText(context, "Подготовка камеры...", Toast.LENGTH_SHORT).show()
            } else if (hasVideoPermissions) {
                val started = viewModel.startVideoRecording(capture, context)
                if (started) {
                    isRecording = true
                    Toast.makeText(context, "Начата запись видео", Toast.LENGTH_SHORT).show()
                }
            } else {
                videoPermissionLauncher.launch(videoPermissions.toTypedArray())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val viewerPhotoUri = viewerUri
        if (viewerPhotoUri != null) {
            MediaViewerScreen(
                uri = viewerPhotoUri,
                media = emptyList(),
                startIndex = 0,
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Зона 1: верхняя полоса — фиксированная высота, топбар прижат к низу
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CameraTopBar(Modifier.padding(bottom = 8.dp))
                }

                // Зона 2: фото — ровно 3:4, без weight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .background(Color.Black)
                ) {
                    CameraPreview(
                        cameraSelector = cameraSelector,
                        isVideoMode = isVideoMode,
                        onCameraReady = { imgCapture, vidCapture ->
                            imageCapture = imgCapture
                            videoCapture = vidCapture
                            Log.wtf("CameraPreview", "onCameraReady called: videoCapture=${vidCapture != null}")
                        }
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
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 12.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
val editInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(35.2.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isEditingOverlay) Color.White.copy(alpha = 0.56f)
                                        else Color.Black.copy(alpha = 0.56f)
                                    )
                                    .clickable(
                                        interactionSource = editInteraction,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = { isEditingOverlay = !isEditingOverlay }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = if (isEditingOverlay) {
                                        "Выключить редактирование"
                                    } else {
                                        "Редактировать"
                                    },
                                    tint = if (isEditingOverlay) {
                                        Color.Black.copy(alpha = 0.56f)
                                    } else {
                                        Color.White.copy(alpha = 0.56f)
                                    },
                                    modifier = Modifier.size(28.6.dp)
                                )
                            }
                            val flipInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(35.2.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.56f))
                                    .clickable(
                                        interactionSource = flipInteraction,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = {
                                            overlayOffset = Offset.Zero
                                            overlayScale = 1f
                                            overlayRotation = 0f
                                            overlayAlpha = 0.5f
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Сбросить ориентир",
                                    tint = Color.White.copy(alpha = 0.56f),
                                    modifier = Modifier.size(28.6.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(35.2.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(Color.Black.copy(alpha = 0.28f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(overlayAlpha)
                                        .align(Alignment.CenterStart)
                                        .background(Color.White.copy(alpha = 0.45f))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                val pillWidth = size.width.toFloat()
                                                var dragged = false
                                                drag(down.id) { change ->
                                                    dragged = true
                                                    change.consume()
                                                    overlayAlpha =
                                                        (change.position.x / pillWidth)
                                                            .coerceIn(0f, 1f)
                                                }
                                                if (!dragged) {
                                                    overlayAlpha =
                                                        (down.position.x / pillWidth)
                                                            .coerceIn(0f, 1f)
                                                }
                                            }
                                        }
                                )
                            }
                            val closeInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(35.2.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.56f))
                                    .clickable(
                                        interactionSource = closeInteraction,
                                        indication = null,
                                        role = Role.Button,
                                        onClick = { referencePhotoUri = null }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Убрать ориентир",
                                    tint = Color.White.copy(alpha = 0.56f),
                                    modifier = Modifier.size(28.6.dp)
                                )
                            }
                        }
                    }
                }
if (isRecording) {
    VideoRecordingIndicator(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 16.dp, top = 140.dp)
    )
}
                }
                // Зона 3: нижняя полоса
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CameraToolbar(
                            centeredModeIndex = centeredModeIndex,
                            onCenteredModeChange = { centeredModeIndex = it },
                            onModeTapped = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CameraNavIcons(
                                onNavigateToMap = onNavigateToMap,
                                onNavigateToMasterFolders = onNavigateToMasterFolders,
                                onOpenGallery = {
                                    pickReferenceLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
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
                            ShutterButton(
                                isVideoMode = isVideoMode,
                                isRecording = isRecording,
                                enabled = !isVideoMode || videoCapture != null,
                                onClick = {
                                    activeMode = CameraMode.entries[centeredModeIndex]
                                    isSlowMotionActive =
                                        activeMode == CameraMode.SLOW_MO
                                    isTimelapseActive =
                                        activeMode == CameraMode.TIMELAPSE
                                    isVideoMode = activeMode == CameraMode.VIDEO
                                    
                                    if (isVideoMode) {
                                        toggleVideoRecording()
                                    } else {
                                        capturePhoto()
                                    }
                                },
                                onLongPress = {}
                            )
                            lastPhotoUri?.let { uri ->
                                LastPhotoThumbnail(
                                    uri = uri,
                                    onClick = { viewerUri = uri },
                                    modifier = Modifier.offset(x = 90.dp)
                                )
                            }
                        }
                    }
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
    isVideoMode: Boolean,
    onCameraReady: (ImageCapture, VideoCapture<Recorder>?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    val imageCapture = remember { ImageCapture.Builder().build() }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recorder by remember { mutableStateOf<Recorder?>(null) }

    LaunchedEffect(Unit) {
        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.UHD, Quality.FHD, Quality.HD)
        )
        recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        videoCapture = VideoCapture.withOutput(recorder!!)
        Log.wtf("CameraPreview", "VideoCapture created and ready: ${videoCapture != null}")
    }

    LaunchedEffect(cameraSelector, isVideoMode) {
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
                    
                    val useCaseGroup = if (isVideoMode && videoCapture != null) {
                        UseCaseGroup.Builder()
                            .setViewPort(viewPort)
                            .addUseCase(preview)
                            .addUseCase(imageCapture)
                            .addUseCase(videoCapture!!)
                            .build()
                    } else {
                        UseCaseGroup.Builder()
                            .setViewPort(viewPort)
                            .addUseCase(preview)
                            .addUseCase(imageCapture)
                            .build()
                    }
                    
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        useCaseGroup
                    )
                    onCameraReady(imageCapture, videoCapture)
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Не удалось открыть камеру", e)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    )
}

@Composable
private fun LastPhotoThumbnail(
    uri: Uri,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White, CircleShape)
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
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(48.dp)
            .pressScale(interactionSource)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.Gray
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShutterButton(
    isVideoMode: Boolean,
    isRecording: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shutterColor = if (isVideoMode) Color(0xFFFF3B30) else Color.White

    Box(
        modifier = modifier
            .size(72.dp)
            .pressScale(interactionSource)
            .clip(CircleShape)
            .border(4.dp, shutterColor, CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongPress
            )
            .alpha(if (enabled) 1f else 0.5f),
        contentAlignment = Alignment.Center
    ) {
        if (isVideoMode && isRecording) {
            // Красный квадрат внутри при записи
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFF3B30))
            )
        } else if (isVideoMode) {
            // Красный кружок с треугольником для старта записи
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30), CircleShape)
            )
        } else {
            // Белый кружок для фото
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun RecordingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "recordingIndicator")
    val blinkAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recordingBlink"
    )

    Box(
        modifier = modifier
            .size(12.dp)
            .alpha(blinkAlpha)
            .clip(CircleShape)
            .background(Color(0xFFFF3B30), CircleShape)
    )
}

@Composable
private fun VideoRecordingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Color(0xFFFF3B30), CircleShape)
    )
}