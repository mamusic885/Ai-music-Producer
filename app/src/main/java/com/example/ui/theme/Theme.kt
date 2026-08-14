package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudioColorScheme = darkColorScheme(
    primary = StudioTurquoise,
    onPrimary = Color(0xFF00382F),
    primaryContainer = StudioDeepTeal,
    onPrimaryContainer = StudioTurquoiseBright,
    secondary = StudioCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = StudioDarkTeal,
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = StudioMint,
    onTertiary = Color(0xFF00391A),
    tertiaryContainer = Color(0xFF005228),
    onTertiaryContainer = Color(0xFF8CF4A9),
    background = StudioBgDark,
    onBackground = StudioTextPrimary,
    surface = StudioSurfaceDark,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceCard,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainer = StudioSurfaceRack,
    outline = StudioSurfaceBorder,
    outlineVariant = Color(0xFF1E2D3B),
    error = StudioVUMeterRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek studio dark
    dynamicColor: Boolean = false, // Preserve bespoke music studio palette
    content: @Composable () -> Unit
) {
    val colorScheme = StudioColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = StudioBgDark.toArgb()
            window.navigationBarColor = StudioBgDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

