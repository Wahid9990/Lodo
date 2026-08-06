package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LudoPrimary,
    onPrimary = LudoOnPrimary,
    primaryContainer = LudoPrimaryContainer,
    onPrimaryContainer = LudoOnPrimaryContainer,
    background = LudoDarkBackground,
    surface = LudoDarkSurface,
    surfaceVariant = LudoDarkSurfaceVariant,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFFCFD8DC)
)

private val LightColorScheme = lightColorScheme(
    primary = LudoPrimary,
    onPrimary = LudoOnPrimary,
    primaryContainer = LudoPrimaryContainer,
    onPrimaryContainer = LudoOnPrimaryContainer,
    background = LudoLightBackground,
    surface = LudoLightSurface,
    surfaceVariant = LudoLightSurfaceVariant,
    onBackground = Color(0xFF1A202C),
    onSurface = Color(0xFF1A202C),
    onSurfaceVariant = Color(0xFF4A5568)
)

@Composable
fun LudoMasterTheme(
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
