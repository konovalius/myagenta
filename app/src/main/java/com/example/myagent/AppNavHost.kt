package com.example.myagent

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myagent.ui.camera.CameraScreen
import com.example.myagent.ui.folders.MasterFoldersScreen
import com.example.myagent.ui.home.HomeScreen
import com.example.myagent.ui.map.MapScreen
import com.example.myagent.ui.onboarding.OnboardingScreen
import com.example.myagent.ui.splash.SplashScreen

object AppRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val CAMERA = "camera?uri={uri}&lat={lat}&lon={lon}"
    const val ONBOARDING = "onboarding"
    const val MAP = "map"
    const val MASTER_FOLDERS = "master-folders"

    fun camera(uri: Uri? = null, lat: Double? = null, lon: Double? = null): String {
        val params = buildList {
            uri?.let { add("uri=" + Uri.encode(it.toString())) }
            lat?.let { add("lat=$it") }
            lon?.let { add("lon=$it") }
        }
        return if (params.isNotEmpty()) "camera?${params.joinToString("&")}" else "camera"
    }
}

@Composable
fun AppNavHost(
    isDark: Boolean,
    onThemeToggle: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH
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
        composable(AppRoutes.HOME) {
            HomeScreen(
                isDark = isDark,
                onThemeToggle = onThemeToggle,
                onOpenMap = { navController.navigate(AppRoutes.MAP) },
                onOpenImage = { uri -> navController.navigate(AppRoutes.camera(uri)) },
                onOpenOnboarding = { navController.navigate(AppRoutes.ONBOARDING) },
                onOpenMasterFolders = { navController.navigate(AppRoutes.MASTER_FOLDERS) }
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
                }
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")
                ?.let { Uri.decode(it) }
                ?.let { Uri.parse(it) }
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
            CameraScreen(
                initialReferenceUri = uri,
                initialLat = lat,
                initialLon = lon
            )
        }
        composable(AppRoutes.ONBOARDING) {
            OnboardingScreen(
                onContinue = {
                    navController.navigate(AppRoutes.camera(null)) {
                        popUpTo(AppRoutes.HOME) { saveState = true }
                        launchSingleTop = true
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
            MasterFoldersScreen(onBack = { navController.popBackStack() })
        }
    }
}