package com.voice0.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Voice0DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    secondary = AccentLight,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLow,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    error = Danger,
)

@Composable
fun Voice0Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = Voice0DarkScheme,  // dark always — matches voice0 design
        typography = Voice0Typography,
        content = content,
    )
}
