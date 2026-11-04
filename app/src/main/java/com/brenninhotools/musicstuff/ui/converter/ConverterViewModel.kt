package com.brenninhotools.musicstuff.ui.converter

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brenninhotools.musicstuff.convert.FlmConversion
import com.brenninhotools.musicstuff.convert.FlpToFlmConverter
import com.brenninhotools.musicstuff.flm.FlmWriter
import com.brenninhotools.musicstuff.flp.FlpArrangement
import com.brenninhotools.musicstuff.flp.FlpFormatException
import com.brenninhotools.musicstuff.flp.FlpParser
import com.brenninhotools.musicstuff.flp.FlpProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface ConverterUiState {
    data object Idle : ConverterUiState
    data object Working : ConverterUiState
    data class Failed(val message: String) : ConverterUiState
    data class Ready(
        val fileName: String,
        val project: FlpProject,
        val arrangement: FlpArrangement?,
        val conversion: FlmConversion,
        val saved: Boolean = false,
        val saveError: String? = null,
    ) : ConverterUiState
}

class ConverterViewModel(application: Application) : AndroidViewModel(application) {
    private val resolver get() = getApplication<Application>().contentResolver
    private val mutableState = MutableStateFlow<ConverterUiState>(ConverterUiState.Idle)

    val state: StateFlow<ConverterUiState> = mutableState.asStateFlow()

    fun open(uri: Uri) {
        mutableState.value = ConverterUiState.Working
        viewModelScope.launch {
            mutableState.value = withContext(Dispatchers.Default) { load(uri) }
        }
    }

    fun selectArrangement(arrangement: FlpArrangement) {
        val current = mutableState.value as? ConverterUiState.Ready ?: return
        mutableState.value = current.copy(
            arrangement = arrangement,
            conversion = FlpToFlmConverter.convert(current.project, arrangement),
            saved = false,
            saveError = null,
        )
    }

    fun save(uri: Uri) {
        val current = mutableState.value as? ConverterUiState.Ready ?: return
        viewModelScope.launch {
            val error = withContext(Dispatchers.IO) { writeFile(uri, current.conversion) }
            mutableState.value = current.copy(saved = error == null, saveError = error)
        }
    }

    private fun load(uri: Uri): ConverterUiState {
        return try {
            val data = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ConverterUiState.Failed("The file couldn't be opened.")
            val project = FlpParser.parse(data)
            val arrangement = FlpToFlmConverter.defaultArrangement(project)
            ConverterUiState.Ready(
                fileName = displayName(uri),
                project = project,
                arrangement = arrangement,
                conversion = FlpToFlmConverter.convert(project, arrangement),
            )
        } catch (error: FlpFormatException) {
            ConverterUiState.Failed(error.message ?: "This file isn't a valid .flp project.")
        } catch (error: IOException) {
            ConverterUiState.Failed("The file couldn't be read: ${error.message}")
        } catch (error: SecurityException) {
            ConverterUiState.Failed("Music Stuff doesn't have permission to read this file.")
        } catch (error: OutOfMemoryError) {
            ConverterUiState.Failed("This file is too large to open on this device.")
        }
    }

    private fun writeFile(uri: Uri, conversion: FlmConversion): String? {
        return try {
            val bytes = FlmWriter.write(conversion.project)
            val stream = resolver.openOutputStream(uri, "wt") ?: return "The file couldn't be created."
            stream.use { it.write(bytes) }
            null
        } catch (error: IOException) {
            "The file couldn't be saved: ${error.message}"
        } catch (error: SecurityException) {
            "Music Stuff doesn't have permission to save there."
        }
    }

    private fun displayName(uri: Uri): String {
        val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "project.flp"
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: fallback
        } catch (error: SecurityException) {
            fallback
        }
    }
}
