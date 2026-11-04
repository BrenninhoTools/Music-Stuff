package com.brenninhotools.musicstuff.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brenninhotools.musicstuff.ui.ConverterController
import com.brenninhotools.musicstuff.ui.GeneratorController

class AppViewModel : ViewModel() {
    private val player = AndroidAudioPlayer()

    val converter = ConverterController(viewModelScope)
    val generator = GeneratorController(viewModelScope, { player })

    override fun onCleared() {
        player.stop()
    }
}
