package app.file_m25.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.file_m25.data.repository.PreferencesRepository

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint
)

private fun createColorScheme(
    primaryColor: Int?,
    isDark: Boolean
) = if (isDark) {
    darkColorScheme(
        primary = primaryColor?.let { Color(it) } ?: md_theme_dark_primary,
        onPrimary = primaryColor?.let { Color(it) }?.let { onPrimaryColor(it) } ?: md_theme_dark_onPrimary,
        primaryContainer = primaryColor?.let { Color(it) }?.let { primaryContainerColor(it, true) } ?: md_theme_dark_primaryContainer,
        onPrimaryContainer = primaryColor?.let { Color(it) }?.let { onPrimaryContainerColor(it, true) } ?: md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        errorContainer = md_theme_dark_errorContainer,
        onErrorContainer = md_theme_dark_onErrorContainer,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        outline = md_theme_dark_outline,
        outlineVariant = md_theme_dark_outlineVariant,
        inverseSurface = md_theme_dark_inverseSurface,
        inverseOnSurface = md_theme_dark_inverseOnSurface,
        inversePrimary = primaryColor?.let { Color(it) } ?: md_theme_dark_inversePrimary,
        surfaceTint = primaryColor?.let { Color(it) } ?: md_theme_dark_surfaceTint
    )
} else {
    lightColorScheme(
        primary = primaryColor?.let { Color(it) } ?: md_theme_light_primary,
        onPrimary = primaryColor?.let { Color(it) }?.let { onPrimaryColor(it) } ?: md_theme_light_onPrimary,
        primaryContainer = primaryColor?.let { Color(it) }?.let { primaryContainerColor(it, false) } ?: md_theme_light_primaryContainer,
        onPrimaryContainer = primaryColor?.let { Color(it) }?.let { onPrimaryContainerColor(it, false) } ?: md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        error = md_theme_light_error,
        onError = md_theme_light_onError,
        errorContainer = md_theme_light_errorContainer,
        onErrorContainer = md_theme_light_onErrorContainer,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        outline = md_theme_light_outline,
        outlineVariant = md_theme_light_outlineVariant,
        inverseSurface = md_theme_light_inverseSurface,
        inverseOnSurface = md_theme_light_inverseOnSurface,
        inversePrimary = primaryColor?.let { Color(it) } ?: md_theme_light_inversePrimary,
        surfaceTint = primaryColor?.let { Color(it) } ?: md_theme_light_surfaceTint
    )
}

private fun onPrimaryColor(primary: Color): Color {
    val luminance = primary.red * 0.299 + primary.green * 0.587 + primary.blue * 0.114
    return if (luminance > 0.5f) Color.Black else Color.White
}

private fun primaryContainerColor(primary: Color, isDark: Boolean): Color {
    return if (isDark) {
        primary.copy(alpha = 0.3f)
    } else {
        primary.copy(alpha = 0.15f)
    }
}

private fun onPrimaryContainerColor(primary: Color, isDark: Boolean): Color {
    return if (isDark) Color.White else Color.Black
}

data class ThemeSettings(
    val themeMode: PreferencesRepository.ThemeMode = PreferencesRepository.ThemeMode.SYSTEM,
    val primaryColor: Int? = null,
    val dynamicColorEnabled: Boolean = true
)

val LocalThemeSettings = staticCompositionLocalOf { ThemeSettings() }

@Composable
fun FileManagerTheme(
    themeSettings: ThemeSettings = ThemeSettings(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val finalDarkTheme = when (themeSettings.themeMode) {
        PreferencesRepository.ThemeMode.LIGHT -> false
        PreferencesRepository.ThemeMode.DARK -> true
        PreferencesRepository.ThemeMode.SYSTEM -> darkTheme
    }

    val colorScheme = when {
        themeSettings.dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (finalDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeSettings.primaryColor != null -> createColorScheme(themeSettings.primaryColor, finalDarkTheme)
        finalDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}