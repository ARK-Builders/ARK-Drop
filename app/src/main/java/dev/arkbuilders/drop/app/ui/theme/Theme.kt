package dev.arkbuilders.drop.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Grey10,
    primaryContainer = Blue40,
    onPrimaryContainer = Grey10,

    secondary = Teal80,
    onSecondary = Grey10,
    secondaryContainer = Teal40,
    onSecondaryContainer = Grey10,

    tertiary = BlueGrey80,
    onTertiary = Grey10,
    tertiaryContainer = BlueGrey40,
    onTertiaryContainer = Grey10,

    error = Red80,
    onError = Grey10,
    errorContainer = Red40,
    onErrorContainer = Grey10,

    background = Grey95,
    onBackground = Grey10,
    surface = SurfaceDark,
    onSurface = Grey10,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Grey30,

    outline = Grey60,
    outlineVariant = Grey70,
    scrim = Color.Black,
    inverseSurface = Grey10,
    inverseOnSurface = Grey90,
    inversePrimary = Blue40,
    surfaceDim = Grey80,
    surfaceBright = Grey70,
    surfaceContainerLowest = Grey95,
    surfaceContainerLow = Grey90,
    surfaceContainer = Grey80,
    surfaceContainerHigh = Grey70,
    surfaceContainerHighest = Grey60
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue80,
    onPrimaryContainer = Grey90,

    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal80,
    onSecondaryContainer = Grey90,

    tertiary = BlueGrey40,
    onTertiary = Color.White,
    tertiaryContainer = BlueGrey80,
    onTertiaryContainer = Grey90,

    error = Red40,
    onError = Color.White,
    errorContainer = Red80,
    onErrorContainer = Grey90,

    background = Grey10,
    onBackground = Grey90,
    surface = SurfaceLight,
    onSurface = Grey90,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Grey70,

    outline = Grey60,
    outlineVariant = Grey40,
    scrim = Color.Black,
    inverseSurface = Grey90,
    inverseOnSurface = Grey10,
    inversePrimary = Blue80,
    surfaceDim = Grey30,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Grey20,
    surfaceContainer = Grey30,
    surfaceContainerHigh = Grey40,
    surfaceContainerHighest = Grey50
)

@Composable
fun DropTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled for consistent branding
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
