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
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.ZoomState
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myagent.ui.theme.GoshaSans
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import coil.compose.AsyncImage
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.ui.common.ShutterUi
import com.example.myagent.ui.common.pressScale
import java.util.Locale

@Composable
fun CameraScreen(
    initialReferenceUri: Uri? = null,
    initialLat: Double? = null,
    initialLon: Double? = null,
    initialFolderUuid: String? = null,
    initialArchiveTitle: String? = null,
    initialArchiveLat: Double? = null,
    initialArchiveLon: Double? = null,
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
    var centeredModeIndex by remember { mutableIntStateOf(0) }
    var activeMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Ряд пресетов зума, посчитанный из реально доступных объективов.
    var zoomPresets by remember { mutableStateOf<List<ZoomPreset>>(emptyList()) }

    // Непрерывное увеличение относительно базового объектива — единственный
    // источник правды по зуму. Дискретный пресет задаёт его значение, пинч
    // двигает между шагами, поэтому оно не обязано совпадать с подписью
    // какого-либо чипа. Объектив и коэффициент для камеры выводятся из него.
    var zoomMagnification by remember { mutableFloatStateOf(1f) }
    // Капсула зума раскрыта — пока это так, по центру превью висит крупное
    // значение увеличения.
    var isZoomScrubbing by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    
    // Определяем isVideoMode на основе текущего выбранного режима
    val isVideoMode = remember(centeredModeIndex) {
        val mode = CameraMode.entries.getOrNull(centeredModeIndex)
        mode == CameraMode.VIDEO || mode == CameraMode.SLOW_MO || mode == CameraMode.TIMELAPSE
    }
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

    LaunchedEffect(initialArchiveTitle, initialArchiveLat, initialArchiveLon, initialReferenceUri) {
        viewModel.setArchiveInfo(
            initialArchiveTitle,
            initialReferenceUri?.toString(),
            initialArchiveLat,
            initialArchiveLon
        )
    }

    LaunchedEffect(referencePhotoUri) {
        viewModel.setReferenceUri(referencePhotoUri)
    }

    LaunchedEffect(savedPhotoEvent) {
        val event = savedPhotoEvent
        if (event != null) {
            onPhotoCapturedFromFolder(event.uri, event.folderUuid)
            viewModel.consumeSavedPhotoEvent()
        }
    }

    val geoPrompt by viewModel.geoPrompt.collectAsState()

    val showArchiveDialog by viewModel.showArchiveDialog.collectAsState()
    if (showArchiveDialog && initialArchiveTitle != null) {
        ArchiveFolderDialog(
            defaultName = initialArchiveTitle,
            onConfirm = { name ->
                viewModel.onArchiveFolderConfirm(name)
            },
            onSkip = {
                Log.wtf("PastVu", "archive: skip folder")
                viewModel.dismissArchiveDialog()
            },
            onDismiss = {
                Log.wtf("PastVu", "archive: skip folder")
                viewModel.dismissArchiveDialog()
            }
        )
    }

    val archiveTarget by viewModel.archiveTarget.collectAsState()
    archiveTarget?.let { target ->
        ArchiveTargetDialog(
            archiveTitle = target.title,
            archiveLat = target.lat,
            archiveLon = target.lon,
            existingFolders = target.folders,
            onSelectExisting = { viewModel.onTargetExisting(it) },
            onCreateFromArchive = { viewModel.onTargetCreateFromArchive(it) },
            onCreateFromGeocoder = { viewModel.onTargetCreateFromGeocoder(it) },
            onCreateCustom = { viewModel.onTargetCreateCustom(it) },
            onDismiss = { viewModel.onTargetCancelled() }
        )
    }

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

    // Разведка объективов: определяем, какие камеры реально отдал CameraX,
    // и строим из них ряд пресетов зума. Пересчитывается при смене направления.
    LaunchedEffect(isFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val selector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                runCatching {
                    val provider = cameraProviderFuture.get()
                    val lensFacing = selector.lensFacing ?: CameraSelector.LENS_FACING_BACK
                    val manager = cameraManagerFrom(context)
                    val baseId = selector.resolveCameraId(provider.availableCameraInfos)
                    val lenses = manager?.lensesFor(lensFacing).orEmpty().filter { lens ->
                        runCatching {
                            provider.hasCamera(lensCameraSelector(selector, lens.cameraId))
                        }.getOrDefault(false)
                    }
                    val baseLens = lenses.firstOrNull { it.cameraId == baseId }
                        ?: lenses.firstOrNull()
                    val presets = baseLens?.let { buildZoomPresets(lenses, it) } ?: emptyList()
                    zoomPresets = presets
                    zoomMagnification = presets
                        .firstOrNull { it.isBaseLens }
                        ?.magnification
                        ?: 1f
                }.onFailure {
                    Log.e("CameraZoom", "Не удалось разобрать объективы", it)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

// Пул объективов берётся из самих пресетов: отдельная копия в состоянии
    // рано или поздно рассинхронизируется с рядом.
    val lensPool = remember(zoomPresets) {
        zoomPresets.map { it.lens }.distinctBy { it.cameraId }
    }
    val baseLens = remember(zoomPresets) {
        zoomPresets.firstOrNull { it.isBaseLens }?.lens
    }

    // Пинч двигает непрерывное увеличение, а объектив и коэффициент зума на
    // нём выводятся отсюда. Переход между объективами поэтому двусторонний:
    // и уход ниже 1x на широкий, и возврат обратно на основной.
    val resolvedZoom = remember(lensPool, baseLens, zoomMagnification) {
        baseLens?.let { resolveZoom(lensPool, it, zoomMagnification) }
    }
    val appliedZoomRatio = resolvedZoom?.zoomRatio ?: 1f
    val magnificationRange = remember(lensPool, baseLens) {
        baseLens?.let { zoomMagnificationRange(lensPool, it) } ?: (1f..1f)
    }

    // Объектив, отличный от базового, требует перебиндовки камеры. На самом
    // базовом перебиндовываться вхолостую незачем.
    val lensCameraId = resolvedZoom
        ?.lens
        ?.takeIf { baseLens == null || it.cameraId != baseLens.cameraId }
        ?.cameraId

// Цифровой зум применяется к текущей камере. Ключ включает boundCamera,
    // поэтому после каждого перебиндинга (смена объектива, флип, режим) зум
    // восстанавливается, а не сбрасывается в 1x.
    LaunchedEffect(boundCamera, appliedZoomRatio) {
        boundCamera?.cameraControl?.setZoomRatio(appliedZoomRatio)
    }

    // Фактический коэффициент зума, который применил HAL. Лог нужен, чтобы
    // проверять пресеты по цифрам, а не на глаз.
    DisposableEffect(boundCamera) {
        val zoomState = boundCamera?.cameraInfo?.zoomState
        val observer = Observer<ZoomState> { state ->
            Log.wtf(
                "CameraZoom",
                "zoom=${state.zoomRatio} min=${state.minZoomRatio} max=${state.maxZoomRatio}"
            )
        }
        zoomState?.observeForever(observer)
        onDispose { zoomState?.removeObserver(observer) }
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
                    Toast.makeText(context, "запись видео", Toast.LENGTH_SHORT).show()
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
            isPaused = false
            Toast.makeText(context, "Запись видео остановлена", Toast.LENGTH_SHORT).show()
        } else {
            val capture = videoCapture
            if (capture == null) {
                Toast.makeText(context, "Подготовка камеры...", Toast.LENGTH_SHORT).show()
            } else if (hasVideoPermissions) {
                val started = viewModel.startVideoRecording(capture, context)
                if (started) {
                    isRecording = true
                    isPaused = false
                    Toast.makeText(context, "Начата запись видео", Toast.LENGTH_SHORT).show()
                }
            } else {
                videoPermissionLauncher.launch(videoPermissions.toTypedArray())
}
    }
}

    val togglePauseRecording = {
        if (isRecording) {
            viewModel.togglePauseRecording()
            isPaused = viewModel.isPaused()
            val message = if (isPaused) "Запись на паузе" else "Запись продолжена"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                        // Пинч зумит превью. Жест отключён, пока правится
                        // ориентир-фото: там щипок масштабирует саму картинку,
                        // и два жеста на одном экране конфликтовали бы.
                        .then(
                            if (isEditingOverlay) {
                                Modifier
                            } else {
                                Modifier.pointerInput(boundCamera, magnificationRange) {
                                    detectTransformGestures(panZoomLock = true) { _, _, gestureZoom, _ ->
                                        // Кламп по суммарному диапазону всех
                                        // объективов: увеличение не уезжает за
                                        // пределы, поэтому подсветка чипа не
                                        // залипает на краю ряда.
                                        zoomMagnification = (zoomMagnification * gestureZoom)
                                            .coerceIn(magnificationRange)
                                    }
                                }
                            }
                        )
                ) {
                    CameraPreview(
                        cameraSelector = cameraSelector,
                        isVideoMode = isVideoMode,
                        lensCameraId = lensCameraId,
                        onCameraReady = { imgCapture, vidCapture, camera ->
                            imageCapture = imgCapture
                            videoCapture = vidCapture
                            boundCamera = camera
                            Log.wtf("CameraPreview", "onCameraReady called: videoCapture=${vidCapture != null} camera=${camera != null}")
                        },
onBindFailed = {
                            // Откат на основной объектив: он есть всегда.
                            zoomPresets.firstOrNull { it.isBaseLens }?.let {
                                zoomMagnification = it.magnification
                            }
                            boundCamera = null
                            Toast.makeText(context, "Объектив недоступен", Toast.LENGTH_SHORT)
                                .show()
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
// Пока тянут линейку, крупное значение стоит по центру кадра: цифры
                    // читаются сразу, не надо искать кончик указателя.
                    if (isZoomScrubbing) {
                        Text(
                            text = "%.1fx".format(zoomMagnification),
                            fontSize = 36.sp,
                            fontFamily = GoshaSans,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
if (isRecording) {
    VideoRecordingIndicator(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = 16.dp, top = 16.dp),
        isPaused = isPaused
    )
}
                    // Капсула зума по нижнему краю превью. В покое показывает «1x» и текущее
                    // значение; удержание раскрывает весь ряд пресетов, и пока
                    // палец не отпущен, протяжка водит зум непрерывно.
                    ZoomPill(
                        presets = zoomPresets,
                        magnification = zoomMagnification,
                        range = magnificationRange,
                        onMagnificationChange = { zoomMagnification = it },
                        onScrubbingChange = { isZoomScrubbing = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .padding(horizontal = 24.dp)
                    )
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
                                },
                                onSelfieTapped = {}
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ShutterUi.BottomPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularIconButton(
                                icon = Icons.Filled.FlipCameraAndroid,
                                contentDescription = "Переключить камеру",
                                enabled = hasFrontCamera,
                                onClick = { isFrontCamera = !isFrontCamera },
                                modifier = Modifier.offset(x = (-68).dp)
                            )
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                ShutterButton(
                                    isVideoMode = isVideoMode,
                                    isRecording = isRecording,
                                    isPaused = isPaused,
                                    enabled = !isVideoMode || videoCapture != null,
                                    onClick = {
                                        activeMode = CameraMode.entries[centeredModeIndex]
                                        isSlowMotionActive =
                                            activeMode == CameraMode.SLOW_MO
                                        isTimelapseActive =
                                            activeMode == CameraMode.TIMELAPSE
                                        
                                        if (isVideoMode) {
                                            toggleVideoRecording()
                                        } else {
                                            capturePhoto()
                                        }
                                    },
                                    onLongPress = {}
                                )
                                
                                // Значок паузы справа от кнопки во время записи видео.
                                // Размер узла строго ShutterUi.Size, иначе строка с кнопкой
                                // съёмки становится выше и кнопка съёмки смещается вверх.
                                if (isVideoMode && isRecording) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 80.dp)
                                            .size(ShutterUi.Size)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                togglePauseRecording()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PauseButtonWithPulse(
                                            modifier = Modifier.fillMaxSize(),
                                            isPaused = isPaused
                                        )
                                    }
                                }
                            }
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
    lensCameraId: String?,
    onCameraReady: (ImageCapture, VideoCapture<Recorder>?, Camera?) -> Unit = { _, _, _ -> },
    onBindFailed: () -> Unit = {}
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

    LaunchedEffect(cameraSelector, isVideoMode, lensCameraId) {
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
                    
                    val selector = lensCameraId?.let { lensCameraSelector(cameraSelector, it) }
                        ?: cameraSelector

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        useCaseGroup
                    )
                    onCameraReady(imageCapture, videoCapture, camera)
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Не удалось открыть камеру", e)
                    onBindFailed()
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

/**
 * Селектор конкретного физического обътива.
 *
 * Широкий угол на части устройств недостижим через [androidx.camera.core.CameraControl.setZoomRatio]
 * (там minZoomRatio = 1.0), поэтому объектив выбирается прямой привязкой к cameraId.
 * Направление камеры наследуется от базового селектора, чтобы wide нельзя было выбрать
 * на фронтальной камере.
 */
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
    isPaused: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shutterColor = Color.White
    val redDotColor = Color(0xFFFF3B30)
    val redSquareColor = Color(0xFFFF3B30)
    val density = LocalDensity.current
    
    // Анимация бегущего отрезка (10 секунд на полный оборот)
    val runningSegmentAngle = remember { Animatable(0f) }
    
    // Запускаем/останавливаем анимацию в зависимости от состояния паузы
    LaunchedEffect(isVideoMode, isRecording, isPaused) {
        if (isVideoMode && isRecording && !isPaused) {
            // Бесконечная анимация при записи и не на паузе
            while (true) {
                runningSegmentAngle.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(
                        durationMillis = 10000,
                        easing = LinearEasing
                    )
                )
                runningSegmentAngle.snapTo(0f)
            }
        } else if (!isVideoMode || !isRecording) {
            // Если не записываем, сбрасываем угол
            runningSegmentAngle.snapTo(0f)
        }
        // При паузе (isVideoMode && isRecording && isPaused) - ничего не делаем,
        // угол остаётся на текущем значении
    }
    
    Box(
        modifier = modifier
            .size(ShutterUi.Size)
            .pressScale(interactionSource)
            .clip(CircleShape)
            .border(ShutterUi.BorderWidth, shutterColor, CircleShape)
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
            // Состояние во время записи видео
            Box(
                modifier = Modifier.fillMaxSize()
                    .drawBehind {
                        // Рисуем 60 засечек по краю
                        val center = this.center
                        val radius = this.size.minDimension / 2 - with(density) { 4.dp.toPx() }
                        val notchCount = 60
                        val notchLength = with(density) { 4.dp.toPx() }
                        
                        for (i in 0 until notchCount) {
                            val angle = (i * 360f / notchCount) * (Math.PI / 180).toFloat()
                            val startX = center.x + (radius - notchLength) * cos(angle)
                            val startY = center.y + (radius - notchLength) * sin(angle)
                            val endX = center.x + radius * cos(angle)
                            val endY = center.y + radius * sin(angle)
                            
                            drawLine(
                                color = Color.White.copy(alpha = 0.3f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = with(density) { 1.dp.toPx() }
                            )
                        }
                        
                        // Рисуем бегущий отрезок (черная черточка) только при записи
                        if (isVideoMode && isRecording) {
                            val angleRad = runningSegmentAngle.value * (Math.PI / 180).toFloat()
                            val segmentLength = with(density) { 8.dp.toPx() }
                            val segmentWidth = with(density) { 2.dp.toPx() }
                            
                            // Начало черточки на краю круга
                            val startX = center.x + radius * cos(angleRad)
                            val startY = center.y + radius * sin(angleRad)
                            // Конец черточки внутрь круга
                            val endX = center.x + (radius - segmentLength) * cos(angleRad)
                            val endY = center.y + (radius - segmentLength) * sin(angleRad)
                            
                            drawLine(
                                color = Color.White,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = segmentWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
            ) {
                // Красный квадратик по центру (6.dp)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(redSquareColor)
                )
            }
        } else if (isVideoMode && !isRecording) {
            // Видео режим до записи - белый круг с красной точкой
            Box(
                modifier = Modifier
                    .size(ShutterUi.InnerSize)
                    .clip(CircleShape)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Красная точка 6.dp в центре
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(redDotColor)
                )
            }
        } else {
            // Фото режим - белый круг
            Box(
                modifier = Modifier
                    .size(ShutterUi.InnerSize)
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
private fun VideoRecordingIndicator(
    modifier: Modifier = Modifier,
    isPaused: Boolean
) {
    // Анимация мигания для индикатора
    val infiniteTransition = rememberInfiniteTransition(label = "blinkAnimation")
    val alphaAnimation = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )
    
    // Цвет в зависимости от состояния
    val indicatorColor = if (isPaused) Color.Yellow else Color(0xFFFF3B30)
    
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(
                indicatorColor.copy(alpha = alphaAnimation.value),
                CircleShape
            )
    )
}

@Composable
fun PauseIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Две вертикальные полоски (увеличены в 2 раза)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(2.dp))
        }
    }
}

@Composable
fun ArchiveFolderDialog(
    defaultName: String,
    onConfirm: (String) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать новую папку?") },
        text = {
            Column {
                Text("Название папки:")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("Да") }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Нет") }
        }
    )
}

@Composable
fun ArchiveTargetDialog(
    archiveTitle: String,
    archiveLat: Double?,
    archiveLon: Double?,
    existingFolders: List<MasterFolder>,
    onSelectExisting: (MasterFolder) -> Unit,
    onCreateFromArchive: (String) -> Unit,
    onCreateFromGeocoder: (String) -> Unit,
    onCreateCustom: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomName by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    val coordsText = remember(archiveLat, archiveLon) {
        if (archiveLat != null && archiveLon != null) {
            String.format(Locale.US, "%.4f, %.4f", archiveLat, archiveLon)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Куда сохранить?") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (existingFolders.isNotEmpty()) {
                    existingFolders.forEach { folder ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectExisting(folder) }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "существующая",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                TextButton(
                    onClick = { onCreateFromArchive(archiveTitle) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Создать: $archiveTitle")
                }
                if (coordsText != null) {
                    TextButton(
                        onClick = { onCreateFromGeocoder(coordsText) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Создать: $coordsText")
                    }
                }
                TextButton(
                    onClick = { showCustomName = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Своё название")
                }
                if (showCustomName) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        singleLine = true,
                        placeholder = { Text("Название папки") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                    if (customName.isNotBlank()) {
                        TextButton(onClick = { onCreateCustom(customName.trim()) }) {
                            Text("Создать")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}