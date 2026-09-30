package com.example.myagent

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myagent.ui.camera.CameraScreen
import com.example.myagent.ui.folders.MasterFolderContentScreen
import com.example.myagent.ui.folders.MasterFoldersScreen
import com.example.myagent.ui.folders.PhotoViewerViewModel
import com.example.myagent.ui.map.MapScreen
import com.example.myagent.ui.onboarding.OnboardingScreen
import com.example.myagent.ui.splash.SplashScreen
import com.example.myagent.ui.camera.MediaViewerScreen
import com.example.myagent.ui.media.AllMediaScreen
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

    fun mediaViewer(
        uri: Uri,
        folderUuid: String,
        showUseButton: Boolean = true,
        startIndex: Int = 0
    ): String =
        "media-viewer?uri=" + Uri.encode(uri.toString()) +
            "&folderUuid=" + folderUuid +
            "&showUseButton=" + showUseButton +
            "&startIndex=" + startIndex

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
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    
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
                }
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
                onBackClick = { navController.popBackStack() }
            )
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
                    navController.navigate(AppRoutes.mediaViewer(uri, folderUuid, startIndex = index))
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
            if (uri != null) {
                MediaViewerScreen(
                    uri = uri,
                    media = media,
                    startIndex = startIndex,
                    onUseMedia = if (showUseButton && folderUuid != null) {
                        { currentUri ->
                            navController.navigate(
                                AppRoutes.camera(uri = currentUri, folderUuid = folderUuid)
                            )
                        }
                    } else {
                        null
                    },
                    onSaveMedia = if (showUseButton && folderUuid != null) {
                        {
                            scope.launch {
                                navController.navigate(AppRoutes.camera()) {
                                    popUpTo(AppRoutes.MASTER_FOLDER_CONTENT) { inclusive = true }
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