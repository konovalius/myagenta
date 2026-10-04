package com.example.myagent

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myagent.data.util.PastVuPhoto
import com.example.myagent.ui.camera.CameraScreen
import com.example.myagent.ui.folders.MasterFolderContentScreen
import com.example.myagent.ui.folders.MasterFoldersScreen
import com.example.myagent.ui.folders.PhotoViewerViewModel
import com.example.myagent.ui.map.MapScreen
import com.example.myagent.ui.map.ArchivePhotoViewerScreen
import com.example.myagent.ui.onboarding.OnboardingScreen
import com.example.myagent.ui.splash.SplashScreen
import com.example.myagent.ui.camera.MediaViewerScreen
import com.example.myagent.ui.media.AllMediaScreen
import com.example.myagent.ui.media.VideoPlayerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AppRoutes {
    const val SPLASH = "splash"
    const val CAMERA = "camera?uri={uri}&lat={lat}&lon={lon}&folderUuid={folderUuid}"
    const val ONBOARDING = "onboarding"
    const val MAP = "map"
    const val MASTER_FOLDERS = "master-folders"
    const val MASTER_FOLDER_CONTENT = "master-folder/{folderUuid}"
    const val ALL_MEDIA = "all-media"
    const val MEDIA_VIEWER = "media-viewer?uri={uri}&folderUuid={folderUuid}&showUseButton={showUseButton}&startIndex={startIndex}"
    const val VIDEO_PLAYER = "video-player?uri={uri}"
    const val ARCHIVE_PHOTO = "archive-photo?file={file}&title={title}&year={year}&year2={year2}"

    fun archivePhoto(file: String, title: String, year: Int, year2: Int): String {
        val params = buildList {
            add("file=" + Uri.encode(file))
            add("title=" + Uri.encode(title))
            add("year=$year")
            add("year2=$year2")
        }
        return "archive-photo?" + params.joinToString("&")
    }

    fun mediaViewer(
        uri: Uri,
        folderUuid: String? = null,
        showUseButton: Boolean = true,
        startIndex: Int = 0
    ): String {
        val params = buildList {
            add("uri=" + Uri.encode(uri.toString()))
            folderUuid?.let { add("folderUuid=$it") }
            add("showUseButton=$showUseButton")
            add("startIndex=$startIndex")
        }
        return "media-viewer?" + params.joinToString("&")
    }

    fun camera(
        uri: Uri? = null,
        lat: Double? = null,
        lon: Double? = null,
        folderUuid: String? = null
    ): String {
        val params = buildList {
            uri?.let { add("uri=${it.toString()}") }
            lat?.let { add("lat=$it") }
            lon?.let { add("lon=$it") }
            folderUuid?.let { add("folderUuid=$it") }
        }
        return if (params.isNotEmpty()) "camera?${params.joinToString("&")}" else "camera"
    }

    fun videoPlayer(uri: Uri): String =
        "video-player?uri=" + Uri.encode(uri.toString())
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val isPreviewOpen = remember { mutableStateOf(false) }

    fun openSinglePreview(route: String) {
        if (isPreviewOpen.value) return
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == AppRoutes.MEDIA_VIEWER || currentRoute == AppRoutes.VIDEO_PLAYER) return
        isPreviewOpen.value = true
        try {
            navController.navigate(route) { launchSingleTop = true }
        } catch (e: Exception) {
            isPreviewOpen.value = false
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH,
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        composable(AppRoutes.SPLASH) {
            SplashScreen(
                onAnimationComplete = {
                    navController.navigate(AppRoutes.camera()) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = AppRoutes.CAMERA,
            arguments = listOf(
                navArgument("uri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lat") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lon") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("folderUuid") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")
                ?.let { Uri.decode(it) }
                ?.let { Uri.parse(it) }
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
            val folderUuid = backStackEntry.arguments?.getString("folderUuid")
            CameraScreen(
                initialReferenceUri = uri,
                initialLat = lat,
                initialLon = lon,
                initialFolderUuid = folderUuid,
                onNavigateToMasterFolders = { navController.navigate(AppRoutes.MASTER_FOLDERS) },
                onNavigateToMap = { navController.navigate(AppRoutes.MAP) },
                onNavigateToOnboarding = { navController.navigate(AppRoutes.ONBOARDING) },
                onPhotoCapturedFromFolder = { savedUri, folderUuid ->
                    navController.navigate(
                        AppRoutes.mediaViewer(savedUri, folderUuid, showUseButton = true)
                    ) {
                        popUpTo(AppRoutes.MASTER_FOLDER_CONTENT)
                    }
                }
            )
        }
        composable(AppRoutes.ONBOARDING) {
            OnboardingScreen(
                onContinue = {
                    navController.navigate(AppRoutes.camera(null)) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoutes.MAP) {
            MapScreen(
                onBack = { navController.popBackStack() },
                onOpenCamera = { lat: Double?, lon: Double? ->
                    navController.navigate(AppRoutes.camera(lat = lat, lon = lon))
                },
                onOpenCameraToFolder = { lat: Double?, lon: Double?, folderUuid: String? ->
                    navController.navigate(
                        AppRoutes.camera(lat = lat, lon = lon, folderUuid = folderUuid)
                    )
                },
                onOpenFolder = { folderUuid: String ->
                    navController.navigate("master-folder/$folderUuid")
                },
                onOpenArchivePhoto = { photo: PastVuPhoto ->
                    navController.navigate(
                        AppRoutes.archivePhoto(photo.file, photo.title, photo.year, photo.year2)
                    )
                }
            )
        }
        composable(
            route = AppRoutes.ARCHIVE_PHOTO,
            arguments = listOf(
                navArgument("file") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("year") {
                    type = NavType.StringType
                    defaultValue = "0"
                },
                navArgument("year2") {
                    type = NavType.StringType
                    defaultValue = "0"
                }
            )
        ) { backStackEntry ->
            val file = backStackEntry.arguments?.getString("file").orEmpty()
            val title = backStackEntry.arguments?.getString("title").orEmpty()
            val year = backStackEntry.arguments?.getString("year")?.toIntOrNull() ?: 0
            val year2 = backStackEntry.arguments?.getString("year2")?.toIntOrNull() ?: 0
            ArchivePhotoViewerScreen(
                file = Uri.decode(file),
                title = Uri.decode(title),
                year = year,
                year2 = year2,
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppRoutes.MASTER_FOLDERS) {
            MasterFoldersScreen(
                onBack = { navController.popBackStack() },
                onOpenFolder = { folderUuid ->
                    navController.navigate("master-folder/$folderUuid")
                },
                onOpenAllMedia = { navController.navigate(AppRoutes.ALL_MEDIA) }
            )
        }
        composable(AppRoutes.ALL_MEDIA) {
            AllMediaScreen(
                onBackClick = { navController.popBackStack() },
                onOpenVideo = { uri ->
                    openSinglePreview(AppRoutes.videoPlayer(uri))
                },
                onOpenPhoto = { uri, index ->
                    openSinglePreview(AppRoutes.mediaViewer(uri, null, showUseButton = true, startIndex = index))
                }
            )
        }
        composable(
            route = AppRoutes.VIDEO_PLAYER,
            arguments = listOf(
                navArgument("uri") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")
                ?.let { Uri.decode(it) }
                ?.let { Uri.parse(it) }
            if (uri != null) {
                VideoPlayerScreen(
                    videoUri = uri,
                    onBack = { navController.popBackStack() }
                )
            }
            DisposableEffect(Unit) {
                onDispose { isPreviewOpen.value = false }
            }
        }
        composable(
            route = AppRoutes.MASTER_FOLDER_CONTENT,
            arguments = listOf(
                navArgument("folderUuid") {
                    type = NavType.StringType
                }
            )
        ) {
            MasterFolderContentScreen(
                onBack = { navController.popBackStack() },
                onOpenMedia = { uri, folderUuid, index ->
                    openSinglePreview(AppRoutes.mediaViewer(uri, folderUuid, startIndex = index))
                },
                onOpenVideo = { uri ->
                    openSinglePreview(AppRoutes.videoPlayer(uri))
                }
            )
        }
        composable(
            route = AppRoutes.MEDIA_VIEWER,
            arguments = listOf(
                navArgument("uri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("folderUuid") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("showUseButton") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("startIndex") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")
                ?.let { Uri.decode(it) }
                ?.let { Uri.parse(it) }
            val folderUuid = backStackEntry.arguments?.getString("folderUuid")
            val showUseButton = backStackEntry.arguments?.getString("showUseButton")?.toBoolean() ?: false
            val startIndex = backStackEntry.arguments?.getString("startIndex")?.toIntOrNull() ?: 0
            val viewerViewModel: PhotoViewerViewModel = hiltViewModel()
            val media by viewerViewModel.media.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            DisposableEffect(Unit) {
                onDispose { isPreviewOpen.value = false }
            }
            if (uri != null) {
                MediaViewerScreen(
                    uri = uri,
                    media = media,
                    startIndex = startIndex,
                    onUseMedia = if (showUseButton) {
                        { currentUri ->
                            navController.navigate(
                                AppRoutes.camera(uri = currentUri, folderUuid = folderUuid)
                            )
                        }
                    } else {
                        null
                    },
                    onSaveMedia = if (showUseButton) {
                        {
                            scope.launch {
                                if (folderUuid != null) {
                                    navController.navigate(AppRoutes.camera()) {
                                        popUpTo(AppRoutes.MASTER_FOLDER_CONTENT) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(AppRoutes.camera()) {
                                        popUpTo(AppRoutes.ALL_MEDIA) { inclusive = true }
                                    }
                                }
                            }
                        }
                    } else {
                        null
                    },
                    onBack = { navController.popBackStack() },
                    onDelete = { uriToDelete ->
                        scope.launch(Dispatchers.IO) {
                            viewerViewModel.deleteMedia(uriToDelete)
                            withContext(Dispatchers.Main) {
                                navController.popBackStack()
                            }
                        }
                    }
                )
            }
        }
    }
}