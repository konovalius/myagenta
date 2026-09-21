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

object AppRoutes {
    const val HOME = "home"
    const val CAMERA = "camera?uri={uri}"
    const val ONBOARDING = "onboarding"
    const val MAP = "map"
    const val MASTER_FOLDERS = "master-folders"

    fun camera(uri: Uri?): String {
        val encoded = uri?.let { Uri.encode(it.toString()) }
        return if (encoded != null) "camera?uri=$encoded" else "camera"
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        composable(AppRoutes.HOME) {
            HomeScreen(
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
                }
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri")
                ?.let { Uri.decode(it) }
                ?.let { Uri.parse(it) }
            CameraScreen(initialReferenceUri = uri)
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
            MapScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.MASTER_FOLDERS) {
            MasterFoldersScreen(onBack = { navController.popBackStack() })
        }
    }
}