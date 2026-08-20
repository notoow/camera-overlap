package com.wootan.ghostcamera.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GhostTeal = Color(0xFF2ED6C2)
val CaptureAmber = Color(0xFFFFC04A)
val CameraBlack = Color(0xFF07090A)
val PanelBlack = Color(0xE614191B)
val SoftWhite = Color(0xFFF4F7F7)

private val GhostCameraColors = darkColorScheme(
    primary = GhostTeal,
    onPrimary = Color(0xFF00201C),
    secondary = CaptureAmber,
    onSecondary = Color(0xFF2A1A00),
    background = CameraBlack,
    onBackground = SoftWhite,
    surface = Color(0xFF14191B),
    onSurface = SoftWhite,
    surfaceVariant = Color(0xFF283033),
    onSurfaceVariant = Color(0xFFD7E0E0),
    error = Color(0xFFFFB4AB),
)

@Composable
fun GhostCameraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GhostCameraColors,
        content = content,
    )
}
