package com.wootan.ghostcamera.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ClinicalCyan = Color(0xFFA9DDE0)
val CameraBlack = Color(0xFF050505)
val CameraChrome = Color(0xE6000000)
val SoftWhite = Color(0xFFF5F7F7)

private val GhostCameraColors = darkColorScheme(
    primary = ClinicalCyan,
    onPrimary = Color(0xFF102123),
    secondary = Color(0xFFD5DADA),
    onSecondary = Color(0xFF1A1C1C),
    background = CameraBlack,
    onBackground = SoftWhite,
    surface = Color(0xFF121313),
    onSurface = SoftWhite,
    surfaceVariant = Color(0xFF292B2B),
    onSurfaceVariant = Color(0xFFBDC2C2),
    outline = Color(0xFF666A6A),
    error = Color(0xFFFFB4AB),
)

@Composable
fun GhostCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GhostCameraColors,
        content = content,
    )
}
