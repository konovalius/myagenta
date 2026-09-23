package com.example.myagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myagent.ui.theme.MyAgentTheme
import com.example.myagent.ui.theme.ThemeMode
import com.example.myagent.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppWithSplashOnResume()
        }
    }
}

@Composable
fun AppWithSplashOnResume() {
    var shouldNavigateToSplash by remember { mutableStateOf(false) }
    var isFirstStart by remember { mutableStateOf(true) }
    
    DisposableEffect(ProcessLifecycleOwner.get().lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (!isFirstStart) {
                    shouldNavigateToSplash = true
                } else {
                    isFirstStart = false
                }
            }
        }
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        
        onDispose {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        }
    }
    
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    MyAgentTheme(darkTheme = darkTheme) {
        AppNavHost(
            isDark = darkTheme,
            onThemeToggle = { themeViewModel.toggleTheme(darkTheme) },
            shouldNavigateToSplash = shouldNavigateToSplash,
            onSplashNavigated = { shouldNavigateToSplash = false }
        )
    }
}