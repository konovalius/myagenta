package com.example.myagent

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myagent.ui.camera.CameraScreen
import com.example.myagent.ui.folders.MasterFolderContentScreen
import com.example.myagent.ui.folders.MasterFoldersScreen
import com.example.myagent.ui.map.MapScreen
import com.example.myagent.ui.onboarding.OnboardingScreen
import com.example.myagent.ui.splash.SplashScreen

object AppRoutes {
    const val SPLASH = "splash"
    const val CAMERA = "camera?uri={uri}&lat={lat}&lon={lon}&folderUuid={folderUuid}"
    const val ONBOARDING = "onboarding"
    const val MAP = "map"
    const val MASTER_FOLDERS = "master-folders"
    const val MASTER_FOLDER_CONTENT = "master-folder/{folderUuid}"

    fun camera(
        uri: Uri? = null,
        lat: Double? = null,
        lon: Double? = null,
        folderUuid: String? = null
    ): String {
        val params = buildList {
            uri?.let { add("uri=" + Uri.encode(it.toString())) }
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
                onNavigateToOnboarding = { navController.navigate(AppRoutes.ONBOARDING) }
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
                onOpenCamera = { lat, lon ->
                    navController.navigate(AppRoutes.camera(lat = lat, lon = lon))
                }
            )
        }
        composable(AppRoutes.MASTER_FOLDERS) {
            MasterFoldersScreen(
                onBack = { navController.popBackStack() },
                onOpenFolder = { folderUuid ->
                    navController.navigate("master-folder/$folderUuid")
                }
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
                onOpenCamera = { uri, folderUuid ->
                    navController.navigate(AppRoutes.camera(uri = uri, folderUuid = folderUuid))
                }
            )
        }
    }
}