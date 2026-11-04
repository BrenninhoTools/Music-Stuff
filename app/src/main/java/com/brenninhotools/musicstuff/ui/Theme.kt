package com.brenninhotools.musicstuff.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val Aubergine = Color(0xFF1F0D3D)
val Panel = Color(0xFF2F1860)
val PadOff = Color(0xFF43258A)
val Sun = Color(0xFFFFD23F)
val Pink = Color(0xFFFF4FA3)
val Cyan = Color(0xFF3DE3E0)
val Cream = Color(0xFFFFF8E7)
val Lilac = Color(0xFFC6B6EC)
val Coral = Color(0xFFFF9C8A)

val ChannelColors = listOf(Sun, Pink, Cyan)

private val Colors = darkColorScheme(
    primary = Sun,
    onPrimary = Aubergine,
    secondary = Pink,
    onSecondary = Aubergine,
    secondaryContainer = Sun,
    onSecondaryContainer = Aubergine,
    background = Aubergine,
    onBackground = Cream,
    surface = Panel,
    onSurface = Cream,
    surfaceVariant = PadOff,
    onSurfaceVariant = Lilac,
    outline = Lilac,
    outlineVariant = PadOff,
    error = Coral,
)

private fun type(font: FontFamily) = Typography(
    displaySmall = TextStyle(fontFamily = font, fontSize = 38.sp, lineHeight = 44.sp),
    headlineSmall = TextStyle(fontFamily = font, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = font, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = font, fontSize = 18.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontFamily = font, fontSize = 17.sp, lineHeight = 22.sp),
)

@Composable
fun MusicStuffTheme(font: FontFamily, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = type(font), content = content)
}
