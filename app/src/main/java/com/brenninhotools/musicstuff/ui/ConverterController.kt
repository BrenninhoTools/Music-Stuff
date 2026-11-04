package com.brenninhotools.musicstuff.ui

import com.brenninhotools.musicstuff.convert.FlmLayout
import com.brenninhotools.musicstuff.convert.FlmToFlpConverter
import com.brenninhotools.musicstuff.convert.FlpToFlmConverter
import com.brenninhotools.musicstuff.flm.FlmDocument
import com.brenninhotools.musicstuff.flm.FlmFormatException
import com.brenninhotools.musicstuff.flm.FlmParser
import com.brenninhotools.musicstuff.flm.FlmProject
import com.brenninhotools.musicstuff.flm.FlmWriter
import com.brenninhotools.musicstuff.flp.FlpArrangement
import com.brenninhotools.musicstuff.flp.FlpFormatException
import com.brenninhotools.musicstuff.flp.FlpParser
import com.brenninhotools.musicstuff.flp.FlpProject
import com.brenninhotools.musicstuff.flp.FlpWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

enum class ConvertDirection(
    val label: String,
    val source: String,
    val target: String,
    val sourceApp: String,
    val targetApp: String,
) {
    FLP_TO_FLM("FLP to FLM", ".flp", ".flm", "FL Studio", "FL Studio Mobile"),
    FLM_TO_FLP("FLM to FLP", ".flm", ".flp", "FL Studio Mobile", "FL Studio"),
}

enum class ConvertStage(val label: String) {
    READING("Reading project"),
    PLACING("Placing notes"),
    WRITING("Writing file"),
}

data class ProjectSummary(
    val title: String,
    val tempo: Double,
    val timeSigNumerator: Int,
    val timeSigDenominator: Int,
    val sourceVersion: String,
    val preview: FlmProject,
    val noteCount: Int,
    val placement: String,
    val warnings: List<String>,
    val arrangements: List<FlpArrangement> = emptyList(),
)

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
        val summary: ProjectSummary,
        val arrangement: FlpArrangement? = null,
        val saved: Boolean = false,
        val saveError: String? = null,
    ) : ConverterState
}

class ConverterController(private val scope: CoroutineScope) {
    private sealed interface Source {
        data class Flp(val project: FlpProject) : Source
        data class Flm(val document: FlmDocument) : Source
    }

    private val mutableState = MutableStateFlow<ConverterState>(ConverterState.Idle)
    private val mutableDirection = MutableStateFlow(ConvertDirection.FLP_TO_FLM)
    private var source: Source? = null

    val state: StateFlow<ConverterState> = mutableState.asStateFlow()
    val direction: StateFlow<ConvertDirection> = mutableDirection.asStateFlow()

    fun setDirection(direction: ConvertDirection) {
        if (direction == mutableDirection.value || mutableState.value is ConverterState.Working) return
        mutableDirection.value = direction
        source = null
        mutableState.value = ConverterState.Idle
    }

    fun open(file: ProjectFile) {
        val direction = mutableDirection.value
        val stages = listOf(ConvertStage.READING, ConvertStage.PLACING)
        scope.launch {
            mutableState.value = try {
                val data = stage(file.name, stages, ConvertStage.READING) { file.read() }
                val ready = stage(file.name, stages, ConvertStage.PLACING) {
                    when (direction) {
                        ConvertDirection.FLP_TO_FLM -> openFlp(file.name, data)
                        ConvertDirection.FLM_TO_FLP -> openFlm(file.name, data)
                    }
                }
                ready
            } catch (error: FlpFormatException) {
                ConverterState.Failed(error.message ?: "This file isn't a valid .flp project.")
            } catch (error: FlmFormatException) {
                ConverterState.Failed(error.message ?: "This file isn't a valid .flm project.")
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
        val project = (source as? Source.Flp)?.project ?: return
        mutableState.value = current.copy(
            summary = flpSummary(project, arrangement),
            arrangement = arrangement,
            saved = false,
            saveError = null,
        )
    }

    fun save(target: SaveTarget) {
        val current = mutableState.value as? ConverterState.Ready ?: return
        val origin = source ?: return
        val stages = listOf(ConvertStage.PLACING, ConvertStage.WRITING)
        scope.launch {
            val error = try {
                val bytes = stage(current.fileName, stages, ConvertStage.PLACING) { encode(origin, current.arrangement) }
                stage(current.fileName, stages, ConvertStage.WRITING) { target.write(bytes) }
                null
            } catch (error: IOException) {
                "The file couldn't be saved: ${error.message}"
            } catch (error: SecurityException) {
                "Music Stuff doesn't have permission to save there."
            }
            mutableState.value = current.copy(saved = error == null, saveError = error)
        }
    }

    private fun encode(origin: Source, arrangement: FlpArrangement?): ByteArray = when (origin) {
        is Source.Flp -> FlmWriter.write(FlpToFlmConverter.convert(origin.project, arrangement).project)
        is Source.Flm -> FlpWriter.write(FlmToFlpConverter.convert(origin.document))
    }

    private fun openFlp(fileName: String, data: ByteArray): ConverterState.Ready {
        if (startsWith(data, FLM_MAGIC)) throw FlpFormatException("This is an .flm project. Switch to FLM to FLP to open it.")
        val project = FlpParser.parse(data)
        val arrangement = FlpToFlmConverter.defaultArrangement(project)
        source = Source.Flp(project)
        return ConverterState.Ready(fileName, flpSummary(project, arrangement), arrangement)
    }

    private fun openFlm(fileName: String, data: ByteArray): ConverterState.Ready {
        if (startsWith(data, FLP_MAGIC)) throw FlmFormatException("This is an .flp project. Switch to FLP to FLM to open it.")
        val document = FlmParser.parse(data)
        source = Source.Flm(document)
        return ConverterState.Ready(fileName, flmSummary(document))
    }

    private fun startsWith(data: ByteArray, magic: String): Boolean =
        data.size >= magic.length && magic.indices.all { data[it].toInt() == magic[it].code }

    private fun flpSummary(project: FlpProject, arrangement: FlpArrangement?): ProjectSummary {
        val conversion = FlpToFlmConverter.convert(project, arrangement)
        return ProjectSummary(
            title = project.title,
            tempo = project.tempo,
            timeSigNumerator = project.timeSigNumerator,
            timeSigDenominator = project.timeSigDenominator,
            sourceVersion = project.version.split('.').take(3).joinToString("."),
            preview = conversion.project,
            noteCount = conversion.noteCount,
            placement = when {
                conversion.noteCount == 0 -> "This project has no notes to convert."
                conversion.layout == FlmLayout.ARRANGEMENT -> "Notes are placed where the playlist puts them."
                else -> "Patterns are placed one after another."
            },
            warnings = (project.warnings + conversion.warnings).distinct(),
            arrangements = project.arrangements.filter { it.patternClips.isNotEmpty() },
        )
    }

    private fun flmSummary(document: FlmDocument): ProjectSummary = ProjectSummary(
        title = document.title,
        tempo = document.tempo,
        timeSigNumerator = document.timeSigNumerator,
        timeSigDenominator = document.timeSigDenominator,
        sourceVersion = document.appVersion,
        preview = document.flattened(),
        noteCount = document.noteCount,
        placement = if (document.noteCount == 0) {
            "This project has no notes to convert."
        } else {
            "Each clip becomes a pattern in the playlist."
        },
        warnings = document.warnings,
    )

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
        const val FLM_MAGIC = "10LF"
        const val FLP_MAGIC = "FLhd"
    }
}
