package com.brenninhotools.musicstuff.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.brenninhotools.musicstuff.ui.ConverterController
import com.brenninhotools.musicstuff.ui.LocalPlatform
import com.brenninhotools.musicstuff.ui.MusicStuffApp
import com.brenninhotools.musicstuff.ui.MusicStuffTheme
import java.awt.Dimension

private const val MIN_WIDTH = 760
private const val MIN_HEIGHT = 560

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Music Stuff",
        icon = painterResource("icon.png"),
        state = rememberWindowState(width = 1080.dp, height = 720.dp),
    ) {
        window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
        val platform = remember { desktopPlatform { window } }
        val scope = rememberCoroutineScope()
        val controller = remember { ConverterController(scope) }
        val state by controller.state.collectAsState()
        CompositionLocalProvider(LocalPlatform provides platform) {
            MusicStuffTheme(platform.font) {
                MusicStuffApp(state, controller)
            }
        }
    }
}
