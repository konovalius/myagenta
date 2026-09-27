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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.myagent.ui.theme.GoshaSans
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

@Composable
fun MapScreen(
    onBack: () -> Unit,
    onOpenCamera: (lat: Double, lon: Double) -> Unit,
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
                    onOpenFolder(pin.folder.uuid)
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