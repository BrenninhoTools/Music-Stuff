package com.brenninhotools.musicstuff.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MusicStuffColors = darkColorScheme(
    primary = Color(0xFFC9A6FF),
    onPrimary = Color(0xFF2B1259),
    primaryContainer = Color(0xFF5B2DB0),
    onPrimaryContainer = Color(0xFFEDE0FF),
    secondary = Color(0xFF9E8FB8),
    background = Color(0xFF171022),
    onBackground = Color(0xFFEDE7F6),
    surface = Color(0xFF171022),
    onSurface = Color(0xFFEDE7F6),
    surfaceVariant = Color(0xFF261C38),
    onSurfaceVariant = Color(0xFFB9AECB),
    surfaceContainer = Color(0xFF211830),
    outlineVariant = Color(0xFF3A2E50),
    error = Color(0xFFFFB4AB),
)

@Composable
fun MusicStuffTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MusicStuffColors, content = content)
}
