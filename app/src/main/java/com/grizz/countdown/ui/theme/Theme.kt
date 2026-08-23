package com.grizz.countdown.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B1B1F),
    onPrimary = Color.White,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFEDEDF2),
    onSurfaceVariant = Color(0xFF46464F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6E1E5),
    onPrimary = Color(0xFF1B1B1F),
    background = Color(0xFF121216),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2A2A31),
    onSurfaceVariant = Color(0xFFC9C5D0)
)

/** Black or white, whichever stays readable on [background]. */
fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF10101A) else Color.White

@Composable
fun GrizzTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
