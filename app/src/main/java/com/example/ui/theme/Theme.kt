package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class ThemeMode(val label: String) {
    DARK("Dark (Mono)"),
    LIGHT("Light"),
    SYSTEM("System")
}

private val MonoDarkColorScheme = darkColorScheme(
    primary = MonoPureWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoElevatedHigh,
    onPrimaryContainer = MonoWhite,
    secondary = MonoGreySecondary,
    onSecondary = MonoBlack,
    secondaryContainer = MonoElevated,
    onSecondaryContainer = MonoWhite,
    tertiary = MonoGreyMuted,
    onTertiary = MonoWhite,
    background = MonoBlack,
    onBackground = MonoWhite,
    surface = MonoCharcoal,
    onSurface = MonoWhite,
    surfaceVariant = MonoElevated,
    onSurfaceVariant = MonoGreySecondary,
    surfaceContainerLowest = MonoBlack,
    surfaceContainerLow = MonoCharcoal,
    surfaceContainer = MonoCharcoal,
    surfaceContainerHigh = MonoElevated,
    surfaceContainerHighest = MonoElevatedHigh,
    outline = MonoBorderDark,
    outlineVariant = MonoElevatedHigh,
    error = MonoError,
    onError = MonoBlack
)

private val MonoLightColorScheme = lightColorScheme(
    primary = MonoInk,
    onPrimary = MonoLightCard,
    primaryContainer = MonoLightElevated,
    onPrimaryContainer = MonoInk,
    secondary = MonoLightSecondary,
    onSecondary = MonoLightCard,
    secondaryContainer = MonoLightElevated,
    onSecondaryContainer = MonoInk,
    tertiary = MonoGreyMuted,
    onTertiary = MonoLightCard,
    background = MonoPaper,
    onBackground = MonoInk,
    surface = MonoLightCard,
    onSurface = MonoInk,
    surfaceVariant = MonoLightElevated,
    onSurfaceVariant = MonoLightSecondary,
    surfaceContainerLowest = MonoLightCard,
    surfaceContainerLow = MonoPaper,
    surfaceContainer = MonoLightCard,
    surfaceContainerHigh = MonoLightElevated,
    surfaceContainerHighest = MonoBorderLight,
    outline = MonoBorderLight,
    outlineVariant = MonoLightElevated,
    error = MonoError,
    onError = MonoLightCard
)

val MonoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MonoBoardTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) MonoDarkColorScheme else MonoLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MonoShapes,
        content = content
    )
}
