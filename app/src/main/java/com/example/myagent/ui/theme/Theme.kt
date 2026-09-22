package com.example.myagent.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GraphitPrimary,
    onPrimary = GraphitOnPrimary,
    secondary = GraphitSecondary,
    tertiary = GraphitTertiary,
    background = GraphitBackground,
    onBackground = GraphitOnBackground,
    surface = GraphitSurface,
    onSurface = GraphitOnSurface,
    surfaceVariant = GraphitSurfaceVariant,
    onSurfaceVariant = GraphitOnSurfaceVariant,
    outline = GraphitOutline
)

private val LightColorScheme = lightColorScheme(
    primary = PosterRed,
    onPrimary = OnPosterRed,
    secondary = BurntOrange,
    tertiary = PosterBlue,
    background = CreamBackground,
    onBackground = CreamOnBackground,
    surface = CreamSurface,
    onSurface = CreamOnSurface,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = CreamOnSurfaceVariant,
    outline = PosterOutline
)

@Composable
fun MyAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}