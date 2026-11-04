package com.brenninhotools.musicstuff.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.brenninhotools.musicstuff.R

val Ink = Color(0xFF1B0B3A)
val Night = Color(0xFF2A1368)
val Grape = Color(0xFF4A22B8)
val GrapeLight = Color(0xFF5F33D6)
val Sun = Color(0xFFFFD23F)
val Paper = Color(0xFFF6F0FF)
val Cream = Color(0xFFFFF8E7)
val Muted = Color(0xFF5B4A7D)
val Danger = Color(0xFFB3123F)

val LilitaOne = FontFamily(Font(R.font.lilita_one))

private val MusicStuffColors = lightColorScheme(
    primary = Sun,
    onPrimary = Ink,
    secondary = Grape,
    onSecondary = Paper,
    secondaryContainer = Sun,
    onSecondaryContainer = Ink,
    tertiary = Grape,
    onTertiary = Paper,
    background = Night,
    onBackground = Cream,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = Muted,
    outline = Ink,
    outlineVariant = Color(0xFFD9CCF2),
    error = Danger,
    onError = Paper,
)

private val MusicStuffType = Typography(
    displayMedium = TextStyle(fontFamily = LilitaOne, fontSize = 52.sp, lineHeight = 56.sp),
    headlineSmall = TextStyle(fontFamily = LilitaOne, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = LilitaOne, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = LilitaOne, fontSize = 19.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontFamily = LilitaOne, fontSize = 18.sp, lineHeight = 24.sp),
)

@Composable
fun MusicStuffTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MusicStuffColors, typography = MusicStuffType, content = content)
}
