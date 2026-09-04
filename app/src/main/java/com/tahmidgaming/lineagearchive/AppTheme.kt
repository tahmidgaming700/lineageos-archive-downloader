package com.tahmidgaming.lineagearchive

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemePreferences {
    private const val PREFS = "appearance_preferences"
    private const val KEY_THEME = "theme_mode"

    fun get(context: Context): ThemeMode {
        return when (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, ThemeMode.SYSTEM.name)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    fun set(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.name)
            .apply()
    }
}

val LocalArchiveDarkTheme = compositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEEFF),
    onPrimaryContainer = Color(0xFF002A4A),
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E3FF),
    onSecondaryContainer = Color(0xFF17124F),
    tertiary = Color(0xFF18864B),
    onTertiary = Color.White,
    background = Color(0xFFF2F5FA),
    onBackground = Color(0xFF101318),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101318),
    surfaceVariant = Color(0xFFE8ECF3),
    onSurfaceVariant = Color(0xFF555C67),
    outline = Color(0xFFB8C0CC),
    outlineVariant = Color(0xFFD7DDE6)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF66B8FF),
    onPrimary = Color(0xFF003254),
    primaryContainer = Color(0xFF004A78),
    onPrimaryContainer = Color(0xFFD7ECFF),
    secondary = Color(0xFFA9A4FF),
    onSecondary = Color(0xFF292267),
    secondaryContainer = Color(0xFF403A83),
    onSecondaryContainer = Color(0xFFE7E3FF),
    tertiary = Color(0xFF75D99B),
    onTertiary = Color(0xFF00391D),
    background = Color(0xFF090B10),
    onBackground = Color(0xFFF0F2F6),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFF0F2F6),
    surfaceVariant = Color(0xFF242933),
    onSurfaceVariant = Color(0xFFB8C0CC),
    outline = Color(0xFF68717E),
    outlineVariant = Color(0xFF3B414B)
)

@Composable
fun ArchiveTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalArchiveDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            content = content
        )
    }
}
