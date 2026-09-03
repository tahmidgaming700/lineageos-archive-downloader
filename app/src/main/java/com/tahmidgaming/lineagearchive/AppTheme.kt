package com.tahmidgaming.lineagearchive

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    secondary = Color(0xFF5856D6),
    tertiary = Color(0xFF34C759),
    background = Color(0xFFF4F6FA),
    surface = Color(0xCCFFFFFF),
    surfaceContainer = Color(0xB8FFFFFF),
    surfaceContainerHigh = Color(0xE6FFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF64B5FF),
    secondary = Color(0xFF9A95FF),
    tertiary = Color(0xFF63E68A),
    background = Color(0xFF080A0F),
    surface = Color(0x661B1D24),
    surfaceContainer = Color(0x7A20232B),
    surfaceContainerHigh = Color(0x99272A33)
)

@Composable
fun ArchiveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
