package com.brenninhotools.musicstuff.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontFamily

interface ProjectFile {
    val name: String
    fun read(): ByteArray
}

fun interface SaveTarget {
    fun write(bytes: ByteArray)
}

class AppPlatform(
    val font: FontFamily,
    val version: String?,
    val pickProject: (onPicked: (ProjectFile) -> Unit) -> Unit,
    val pickSaveTarget: (suggestedName: String, onPicked: (SaveTarget) -> Unit) -> Unit,
    val mascot: @Composable () -> Painter,
    val backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit,
)

val LocalPlatform = staticCompositionLocalOf<AppPlatform> { error("AppPlatform was not provided") }
