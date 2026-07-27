package com.kanagawa.yamada.holodoriinstaller.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = HoloPrimary,
    onPrimary = HoloOnPrimary,
    primaryContainer = HoloPrimaryContainer,
    onPrimaryContainer = HoloOnPrimaryContainer,
    secondary = HoloSecondary,
    onSecondary = HoloOnSecondary,
    secondaryContainer = HoloSecondaryContainer,
    onSecondaryContainer = HoloOnSecondaryContainer,
    tertiary = HoloTertiary,
    onTertiary = HoloOnTertiary,
    tertiaryContainer = HoloTertiaryContainer,
    onTertiaryContainer = HoloOnTertiaryContainer,
    error = HoloError,
    onError = HoloOnError,
    errorContainer = HoloErrorContainer,
    onErrorContainer = HoloOnErrorContainer,
    background = HoloBackground,
    onBackground = HoloOnBackground,
    surface = HoloSurface,
    onSurface = HoloOnSurface,
    surfaceVariant = HoloSurfaceVariant,
    onSurfaceVariant = HoloOnSurfaceVariant,
    outline = HoloOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = HoloPrimaryDark,
    onPrimary = HoloOnPrimaryDark,
    primaryContainer = HoloPrimaryContainerDark,
    onPrimaryContainer = HoloOnPrimaryContainerDark,
    secondary = HoloSecondaryDark,
    onSecondary = HoloOnSecondaryDark,
    secondaryContainer = HoloSecondaryContainerDark,
    onSecondaryContainer = HoloOnSecondaryContainerDark,
    tertiary = HoloTertiaryDark,
    onTertiary = HoloOnTertiaryDark,
    tertiaryContainer = HoloTertiaryContainerDark,
    onTertiaryContainer = HoloOnTertiaryContainerDark,
    error = HoloError,
    onError = HoloOnError,
    errorContainer = HoloErrorContainer,
    onErrorContainer = HoloOnErrorContainer,
    background = HoloBackgroundDark,
    onBackground = HoloOnBackgroundDark,
    surface = HoloSurfaceDark,
    onSurface = HoloOnSurfaceDark,
    surfaceVariant = HoloSurfaceVariantDark,
    onSurfaceVariant = HoloOnSurfaceVariantDark,
    outline = HoloOutlineDark,
)

@Composable
fun HoloDoriInstallerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}