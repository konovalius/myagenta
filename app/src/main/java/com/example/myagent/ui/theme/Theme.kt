package com.example.myagent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GraphitPrimary,
    onPrimary = GraphitOnPrimary,
    primaryContainer = GraphitPrimaryContainer,
    onPrimaryContainer = GraphitOnPrimaryContainer,
    secondary = GraphitSecondary,
    onSecondaryContainer = GraphitOnSecondaryContainer,
    secondaryContainer = GraphitSecondaryContainer,
    tertiary = GraphitTertiary,
    onTertiaryContainer = GraphitOnTertiaryContainer,
    tertiaryContainer = GraphitTertiaryContainer,
    background = GraphitBackground,
    onBackground = GraphitOnBackground,
    surface = GraphitSurface,
    onSurface = GraphitOnSurface,
    surfaceVariant = GraphitSurfaceVariant,
    onSurfaceVariant = GraphitOnSurfaceVariant,
    error = GraphitError,
    errorContainer = GraphitErrorContainer,
    onErrorContainer = GraphitOnErrorContainer,
    outline = GraphitOutline
)

private val LightColorScheme = lightColorScheme(
    primary = PosterRed,
    onPrimary = OnPosterRed,
    primaryContainer = PosterRedContainer,
    onPrimaryContainer = OnPosterRedContainer,
    secondary = BurntOrange,
    onSecondaryContainer = OnBurntOrangeContainer,
    secondaryContainer = BurntOrangeContainer,
    tertiary = PosterBlue,
    onTertiaryContainer = OnPosterBlueContainer,
    tertiaryContainer = PosterBlueContainer,
    background = CreamBackground,
    onBackground = CreamOnBackground,
    surface = CreamSurface,
    onSurface = CreamOnSurface,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = CreamOnSurfaceVariant,
    error = CreamError,
    errorContainer = CreamErrorContainer,
    onErrorContainer = OnCreamErrorContainer,
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