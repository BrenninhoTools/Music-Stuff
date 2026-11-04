package com.brenninhotools.musicstuff.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brenninhotools.musicstuff.ui.ConverterController

class ConverterViewModel : ViewModel() {
    val controller = ConverterController(viewModelScope)
}
