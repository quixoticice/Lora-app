package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LoRaOrange,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF431D08),
    onPrimaryContainer = LoRaAmberLight,
    secondary = LoRaCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = LoRaCyanBright,
    tertiary = LoRaGreen,
    onTertiary = Color.Black,
    background = LoRaDarkBg,
    onBackground = LoRaTextPrimary,
    surface = LoRaSurface,
    onSurface = LoRaTextPrimary,
    surfaceVariant = LoRaSurfaceVariant,
    onSurfaceVariant = LoRaTextSecondary,
    outline = LoRaBorder,
    error = LoRaRed,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = LoRaRedBright
)

private val LightColorScheme = darkColorScheme(
    primary = LoRaOrange,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF431D08),
    onPrimaryContainer = LoRaAmberLight,
    secondary = LoRaCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF083344),
    onSecondaryContainer = LoRaCyanBright,
    tertiary = LoRaGreen,
    onTertiary = Color.Black,
    background = LoRaDarkBg,
    onBackground = LoRaTextPrimary,
    surface = LoRaSurface,
    onSurface = LoRaTextPrimary,
    surfaceVariant = LoRaSurfaceVariant,
    onSurfaceVariant = LoRaTextSecondary,
    outline = LoRaBorder,
    error = LoRaRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent tactical high-contrast theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
