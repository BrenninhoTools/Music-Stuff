package com.brenninhotools.musicstuff

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brenninhotools.musicstuff.android.ConverterViewModel
import com.brenninhotools.musicstuff.android.rememberAndroidPlatform
import com.brenninhotools.musicstuff.ui.LocalPlatform
import com.brenninhotools.musicstuff.ui.MusicStuffApp
import com.brenninhotools.musicstuff.ui.MusicStuffTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val platform = rememberAndroidPlatform()
            val model = viewModel<ConverterViewModel>()
            val state by model.controller.state.collectAsState()
            CompositionLocalProvider(LocalPlatform provides platform) {
                MusicStuffTheme(platform.font) {
                    MusicStuffApp(state, model.controller)
                }
            }
        }
    }
}
