package com.voice0.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Voice0Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    secondary = AccentMuted,
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
fun Voice0Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Voice0Scheme,
        typography = Voice0Typography,
        content = content,
    )
}
