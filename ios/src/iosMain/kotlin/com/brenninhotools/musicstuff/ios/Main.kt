package com.brenninhotools.musicstuff.ios

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import com.brenninhotools.musicstuff.ui.ConverterController
import com.brenninhotools.musicstuff.ui.GeneratorController
import com.brenninhotools.musicstuff.ui.LocalPlatform
import com.brenninhotools.musicstuff.ui.MusicStuffApp
import com.brenninhotools.musicstuff.ui.MusicStuffTheme
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    val platform = remember { iosPlatform() }
    val scope = rememberCoroutineScope()
    val player = remember { IosAudioPlayer() }
    val converter = remember { ConverterController(scope) }
    val generator = remember { GeneratorController(scope, { player }) }
    CompositionLocalProvider(LocalPlatform provides platform) {
        MusicStuffTheme(platform.font) {
            MusicStuffApp(converter, generator)
        }
    }
}
