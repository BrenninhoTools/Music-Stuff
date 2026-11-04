package com.brenninhotools.musicstuff.ui

import com.brenninhotools.musicstuff.convert.FlmConversion
import com.brenninhotools.musicstuff.convert.FlpToFlmConverter
import com.brenninhotools.musicstuff.flm.FlmWriter
import com.brenninhotools.musicstuff.flp.FlpArrangement
import com.brenninhotools.musicstuff.flp.FlpFormatException
import com.brenninhotools.musicstuff.flp.FlpParser
import com.brenninhotools.musicstuff.flp.FlpProject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

enum class ConvertStage(val label: String) {
    READING("Reading project"),
    PLACING("Placing notes"),
    WRITING("Writing .flm"),
}

sealed interface ConverterState {
    data object Idle : ConverterState

    data class Working(
        val fileName: String,
        val stages: List<ConvertStage>,
        val current: ConvertStage,
    ) : ConverterState

    data class Failed(val message: String) : ConverterState

    data class Ready(
        val fileName: String,
        val project: FlpProject,
        val arrangement: FlpArrangement?,
        val conversion: FlmConversion,
        val saved: Boolean = false,
        val saveError: String? = null,
    ) : ConverterState
}

class ConverterController(private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow<ConverterState>(ConverterState.Idle)

    val state: StateFlow<ConverterState> = mutableState.asStateFlow()

    fun open(file: ProjectFile) {
        val stages = listOf(ConvertStage.READING, ConvertStage.PLACING)
        scope.launch {
            mutableState.value = try {
                val project = stage(file.name, stages, ConvertStage.READING) { FlpParser.parse(file.read()) }
                val arrangement = FlpToFlmConverter.defaultArrangement(project)
                val conversion = stage(file.name, stages, ConvertStage.PLACING) {
                    FlpToFlmConverter.convert(project, arrangement)
                }
                ConverterState.Ready(file.name, project, arrangement, conversion)
            } catch (error: FlpFormatException) {
                ConverterState.Failed(error.message ?: "This file isn't a valid .flp project.")
            } catch (error: IOException) {
                ConverterState.Failed("The file couldn't be read: ${error.message}")
            } catch (error: SecurityException) {
                ConverterState.Failed("Music Stuff doesn't have permission to read this file.")
            } catch (error: OutOfMemoryError) {
                ConverterState.Failed("This file is too large to open on this device.")
            }
        }
    }

    fun selectArrangement(arrangement: FlpArrangement) {
        val current = mutableState.value as? ConverterState.Ready ?: return
        mutableState.value = current.copy(
            arrangement = arrangement,
            conversion = FlpToFlmConverter.convert(current.project, arrangement),
            saved = false,
            saveError = null,
        )
    }

    fun save(target: SaveTarget) {
        val current = mutableState.value as? ConverterState.Ready ?: return
        val stages = listOf(ConvertStage.PLACING, ConvertStage.WRITING)
        scope.launch {
            val error = try {
                val conversion = stage(current.fileName, stages, ConvertStage.PLACING) {
                    FlpToFlmConverter.convert(current.project, current.arrangement)
                }
                stage(current.fileName, stages, ConvertStage.WRITING) {
                    target.write(FlmWriter.write(conversion.project))
                }
                null
            } catch (error: IOException) {
                "The file couldn't be saved: ${error.message}"
            } catch (error: SecurityException) {
                "Music Stuff doesn't have permission to save there."
            }
            mutableState.value = current.copy(saved = error == null, saveError = error)
        }
    }

    private suspend fun <T> stage(
        fileName: String,
        stages: List<ConvertStage>,
        stage: ConvertStage,
        work: () -> T,
    ): T {
        mutableState.value = ConverterState.Working(fileName, stages, stage)
        val started = System.currentTimeMillis()
        val result = withContext(Dispatchers.Default) { work() }
        val remaining = MIN_STAGE_MILLIS - (System.currentTimeMillis() - started)
        if (remaining > 0) delay(remaining)
        return result
    }

    private companion object {
        const val MIN_STAGE_MILLIS = 450L
    }
}
