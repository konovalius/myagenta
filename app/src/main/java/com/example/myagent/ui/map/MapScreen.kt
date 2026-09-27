package com.example.myagent.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.example.myagent.data.db.entity.MasterFolder
import com.example.myagent.ui.theme.GoshaSans
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private const val DEFAULT_LAT = 55.7558
private const val DEFAULT_LON = 37.6173
private const val DEFAULT_ZOOM = 15.0
private const val LOCATION_ZOOM = 17.0
private const val GEO_FOLDER_ZOOM = 14.0
private const val PIN_SIZE_DP = 48
private const val LONG_PRESS_MILLIS = 3000L

private data class GeoPickState(
    val lat: Double,
    val lon: Double,
    val folders: List<GeoPickFolder>
)

@Composable
fun MapScreen(
    onBack: () -> Unit,
    onOpenCamera: (lat: Double, lon: Double) -> Unit,
    onOpenCameraToFolder: (lat: Double, lon: Double, folderUuid: String) -> Unit,
    onOpenFolder: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val myTiles = remember {
        XYTileSource(
            "MyAgentTiles",
            0,
            19,
            256,
            ".png",
            arrayOf(
                "https://hefty-mule-2745.konovalius.deno.net/"
            ),
            "© OpenStreetMap"
        )
    }
    val mapView = remember {
        Log.i("OSM", "source=${myTiles.name()} url=${myTiles.getTileURLString(MapTileIndex.getTileIndex(15, 19807, 10238))}")
        MapView(context).apply {
            setTileSource(myTiles)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LON))
        }
    }
    val scope = rememberCoroutineScope()
    val geoPickScope = rememberCoroutineScope()
    var geoPick by remember { mutableStateOf<GeoPickState?>(null) }
    var deleteMode by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<MasterFolder?>(null) }

    fun openGeoCameraToFolder(lat: Double, lon: Double, folderUuid: String?) {
        geoPickScope.launch {
            val targetUuid = folderUuid ?: viewModel.createGeoFolder(lat, lon).uuid
            onOpenCameraToFolder(lat, lon, targetUuid)
        }
    }

    fun handleGeoLongPress(gp: GeoPoint) {
        geoPickScope.launch {
            val folders = viewModel.findGeoFoldersNear(gp.latitude, gp.longitude)
            geoPick = GeoPickState(gp.latitude, gp.longitude, folders)
        }
    }

    remember {
        val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
        var downX = 0f
        var downY = 0f
        var moved = false
        var fired = false
        var job: Job? = null
        mapView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    moved = false
                    fired = false
                    job = scope.launch {
                        delay(LONG_PRESS_MILLIS)
                        if (!moved && !fired) {
                            fired = true
                            val point = mapView.projection.fromPixels(downX.toInt(), downY.toInt())
                            handleGeoLongPress(GeoPoint(point.latitude, point.longitude))
                        }
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (kotlin.math.abs(event.x - downX) > touchSlop ||
                        kotlin.math.abs(event.y - downY) > touchSlop
                    ) {
                        moved = true
                        job?.cancel()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    job?.cancel()
                }
            }
            false
        }
    }
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val cancellationToken = remember { CancellationTokenSource() }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }

    val imageLoader = remember { ImageLoader(context) }
    val geoPins by viewModel.geoFolders.collectAsStateWithLifecycle()
    var locatedMarker by remember { mutableStateOf<Marker?>(null) }
    val geoMarkers = remember { mutableStateListOf<Marker>() }

    fun centerMap(lat: Double, lon: Double) {
        currentLocation = GeoPoint(lat, lon)
        locatedMarker?.let { mapView.overlays.remove(it) }
        mapView.controller.setZoom(LOCATION_ZOOM)
        mapView.controller.setCenter(GeoPoint(lat, lon))
        val marker = Marker(mapView).apply {
            position = GeoPoint(lat, lon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ResourcesCompat.getDrawable(context.resources, android.R.drawable.ic_menu_mylocation, null)
            title = "Я здесь"
        }
        mapView.overlays.add(marker)
        locatedMarker = marker
        mapView.invalidate()
    }

    fun locate() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    centerMap(location.latitude, location.longitude)
                } else {
                    Toast.makeText(context, "Не удалось определить местоположение", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Не удалось определить местоположение", Toast.LENGTH_SHORT).show()
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (LOCATION_PERMISSIONS.all { result[it] == true }) {
            locate()
        } else {
            Toast.makeText(context, "Разрешение на геолокацию не выдано", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (LOCATION_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            locate()
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cancellationToken.cancel()
            mapView.onDetach()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(geoPins, mapView, imageLoader) {
        geoMarkers.forEach { mapView.overlays.remove(it) }
        geoMarkers.clear()
        geoPins.forEach { pin ->
            val lat = pin.folder.lat ?: return@forEach
            val lon = pin.folder.lon ?: return@forEach
            val marker = Marker(mapView).apply {
                position = GeoPoint(lat, lon)
                icon = createPinIcon(context, imageLoader, pin.photoUri)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = pin.folder.name
                setOnMarkerClickListener { _, _ ->
                    if (deleteMode) {
                        folderToDelete = pin.folder
                    } else {
                        onOpenFolder(pin.folder.uuid)
                    }
                    true
                }
            }
            mapView.overlays.add(marker)
            geoMarkers += marker
        }
        if (currentLocation == null && geoPins.isNotEmpty()) {
            val first = geoPins.first()
            mapView.controller.setZoom(GEO_FOLDER_ZOOM)
            mapView.controller.setCenter(GeoPoint(first.folder.lat ?: 0.0, first.folder.lon ?: 0.0))
        }
        mapView.invalidate()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101418))
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 48.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White
            )
        }
        val deleteInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 48.dp)
                .size(35.2.dp)
                .clip(CircleShape)
                .background(
                    if (deleteMode) {
                        Color(0xFFFF3B30)
                    } else {
                        Color.Black.copy(alpha = 0.56f)
                    }
                )
                .clickable(
                    interactionSource = deleteInteraction,
                    indication = null,
                    role = Role.Button,
                    onClick = { deleteMode = !deleteMode }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Режим удаления папок",
                tint = Color.White.copy(alpha = if (deleteMode) 1f else 0.56f),
                modifier = Modifier.size(28.6.dp)
            )
        }
        currentLocation?.let { location ->
            ExtendedFloatingActionButton(
onClick = {
                     onOpenCamera(location.latitude, location.longitude)
                 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 10.dp
                ),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = "Снять фото",
                        fontFamily = GoshaSans,
                        fontSize = 18.sp
                    )
                }
            )
        }
        geoPick?.let { pick ->
            if (pick.folders.isEmpty()) {
                AlertDialog(
                    onDismissRequest = { geoPick = null },
                    title = { Text("Создать точку?", fontFamily = GoshaSans) },
                    text = {
                        Text("Рядом нет гео-папок. Создать новую точку для съёмки?")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                geoPick = null
                                openGeoCameraToFolder(pick.lat, pick.lon, null)
                            }
                        ) {
                            Text("Да")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { geoPick = null }) {
                            Text("Нет")
                        }
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = { geoPick = null },
                    title = { Text("Гео-папки рядом", fontFamily = GoshaSans) },
                    text = {
                        Column {
                            pick.folders.forEach { folder ->
                                TextButton(
                                    onClick = {
                                        geoPick = null
                                        openGeoCameraToFolder(pick.lat, pick.lon, folder.uuid)
                                    }
                                ) {
                                    Text(
                                        String.format(
                                            Locale.ROOT,
                                            "%s — %.0f м",
                                            folder.name,
                                            folder.distanceMeters
                                        )
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                geoPick = null
                                openGeoCameraToFolder(pick.lat, pick.lon, null)
                            }
                        ) {
                            Text("Создать новую точку")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { geoPick = null }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }
        folderToDelete?.let { folder ->
            AlertDialog(
                onDismissRequest = { folderToDelete = null },
                title = { Text("Удалить папку?", fontFamily = GoshaSans) },
                text = {
                    Text("Папка «${folder.name}» и все её фото будут удалены безвозвратно.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            folderToDelete = null
                            geoPickScope.launch { viewModel.deleteFolderWithPhotos(folder) }
                        }
                    ) {
                        Text("Да")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToDelete = null }) {
                        Text("Нет")
                    }
                }
            )
        }
    }
}

private suspend fun createPinIcon(
    context: android.content.Context,
    imageLoader: ImageLoader,
    photoUri: String?
): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (PIN_SIZE_DP * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val radius = size / 2f
    val border = (2 * density).toInt()
    canvas.drawCircle(
        center,
        center,
        radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    )
    val innerR = radius - border
    val clipPath = Path().apply { addCircle(center, center, innerR, Path.Direction.CW) }
    canvas.save()
    canvas.clipPath(clipPath)
    canvas.drawRect(
        0f,
        0f,
        size.toFloat(),
        size.toFloat(),
        Paint().apply { color = android.graphics.Color.rgb(170, 170, 170) }
    )
    if (photoUri != null) {
        val drawable = try {
            imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(photoUri)
                    .size((innerR * 2).toInt())
                    .scale(Scale.FILL)
                    .build()
            ).drawable
        } catch (e: Exception) {
            null
        }
        val bmp = (drawable as? BitmapDrawable)?.bitmap
        val software = bmp?.copy(Bitmap.Config.ARGB_8888, false)
        if (software != null) {
            canvas.drawBitmap(
                software,
                null,
                RectF(center - innerR, center - innerR, center + innerR, center + innerR),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
        }
    } else {
        canvas.drawCircle(
            center,
            center,
            innerR * 0.35f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(80, 80, 80) }
        )
    }
    canvas.restore()
    return BitmapDrawable(context.resources, bitmap)
}