package com.tahmidgaming.lineagearchive

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// One UI-inspired system: generous spacing, large titles and soft, friendly corners.
enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemePreferences {
    private const val PREFS = "appearance_preferences"
    private const val KEY_THEME = "theme_mode"

    fun get(context: Context): ThemeMode = when (
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, ThemeMode.SYSTEM.name)
    ) {
        ThemeMode.LIGHT.name -> ThemeMode.LIGHT
        ThemeMode.DARK.name -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    fun set(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_THEME, mode.name).apply()
    }
}

val LocalArchiveDarkTheme = compositionLocalOf { false }

private val LightColors = lightColorScheme(
    primary = Color(0xFF1677FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEBFF),
    onPrimaryContainer = Color(0xFF002A55),
    secondary = Color(0xFF625BDA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E6FF),
    onSecondaryContainer = Color(0xFF211A5D),
    tertiary = Color(0xFF16824A),
    onTertiary = Color.White,
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF111318),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFE9EDF3),
    onSurfaceVariant = Color(0xFF59616D),
    outline = Color(0xFFB8C1CC),
    outlineVariant = Color(0xFFD8DEE7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF72B9FF),
    onPrimary = Color(0xFF00345A),
    primaryContainer = Color(0xFF07527F),
    onPrimaryContainer = Color(0xFFD9EDFF),
    secondary = Color(0xFFB4AEFF),
    onSecondary = Color(0xFF302A70),
    secondaryContainer = Color(0xFF454080),
    onSecondaryContainer = Color(0xFFE9E6FF),
    tertiary = Color(0xFF78D99D),
    onTertiary = Color(0xFF00391D),
    background = Color(0xFF090B0F),
    onBackground = Color(0xFFF1F3F7),
    surface = Color(0xFF171A20),
    onSurface = Color(0xFFF1F3F7),
    surfaceVariant = Color(0xFF242932),
    onSurfaceVariant = Color(0xFFB8C0CB),
    outline = Color(0xFF68717D),
    outlineVariant = Color(0xFF3B414A)
)

private val ArchiveShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp)
)

private val ArchiveTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        headlineLarge = headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
    )
}

@Composable
fun ArchiveTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalArchiveDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = ArchiveTypography,
            shapes = ArchiveShapes,
            content = content
        )
    }
}
