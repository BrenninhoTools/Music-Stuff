package com.brenninhotools.musicstuff

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.brenninhotools.musicstuff.ui.converter.ConverterScreen
import com.brenninhotools.musicstuff.ui.theme.MusicStuffTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicStuffTheme {
                ConverterScreen()
            }
        }
    }
}
