package com.brenninhotools.musicstuff.ui

import com.brenninhotools.musicstuff.flm.FlmWriter
import com.brenninhotools.musicstuff.generate.GeneratedSong
import com.brenninhotools.musicstuff.generate.PreviewSynth
import com.brenninhotools.musicstuff.generate.SongGenerator
import com.brenninhotools.musicstuff.generate.SongPart
import com.brenninhotools.musicstuff.generate.SongSettings
import com.brenninhotools.musicstuff.generate.SongStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.random.Random

data class GeneratorState(
    val song: GeneratedSong,
    val playing: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

class GeneratorController(
    private val scope: CoroutineScope,
    private val player: () -> AudioPlayer,
    seed: Long = System.currentTimeMillis(),
) {
    private val mutableState = MutableStateFlow(GeneratorState(SongGenerator.generate(SongSettings(seed = seed))))
    private var playback: Job? = null

    val state: StateFlow<GeneratorState> = mutableState.asStateFlow()

    fun setStyle(style: SongStyle) = update { it.copy(style = style, bpm = style.defaultBpm) }

    fun setRoot(root: Int) = update { it.copy(root = root) }

    fun setBpm(bpm: Int) = update { it.copy(bpm = bpm) }

    fun setBars(bars: Int) = update { it.copy(bars = bars) }

    fun newSong() = update { it.copy(seed = Random.nextLong(), partSeeds = emptyMap()) }

    fun shuffle(part: SongPart) = update { it.copy(partSeeds = it.partSeeds + (part to Random.nextLong())) }

    fun togglePlayback() {
        if (mutableState.value.playing) {
            stop()
            return
        }
        val song = mutableState.value.song
        mutableState.value = mutableState.value.copy(playing = true, error = null)
        playback = scope.launch {
            try {
                val samples = withContext(Dispatchers.Default) { PreviewSynth.render(song) }
                player().play(samples, PreviewSynth.SAMPLE_RATE)
                delay(samples.size * MILLIS_PER_SECOND / PreviewSynth.SAMPLE_RATE)
                player().stop()
                mutableState.value = mutableState.value.copy(playing = false)
            } catch (error: IllegalStateException) {
                mutableState.value = mutableState.value.copy(playing = false, error = "Audio preview isn't available on this device.")
            }
        }
    }

    fun stop() {
        playback?.cancel()
        playback = null
        player().stop()
        mutableState.value = mutableState.value.copy(playing = false)
    }

    fun save(target: SaveTarget) {
        stop()
        val song = mutableState.value.song
        mutableState.value = mutableState.value.copy(saving = true, saved = false, error = null)
        scope.launch {
            val error = try {
                val started = System.currentTimeMillis()
                withContext(Dispatchers.Default) { target.write(FlmWriter.write(song.project)) }
                val remaining = MIN_SAVE_MILLIS - (System.currentTimeMillis() - started)
                if (remaining > 0) delay(remaining)
                null
            } catch (error: IOException) {
                "The file couldn't be saved: ${error.message}"
            } catch (error: SecurityException) {
                "Music Stuff doesn't have permission to save there."
            }
            mutableState.value = mutableState.value.copy(saving = false, saved = error == null, error = error)
        }
    }

    private fun update(change: (SongSettings) -> SongSettings) {
        stop()
        val settings = change(mutableState.value.song.settings)
        mutableState.value = GeneratorState(SongGenerator.generate(settings))
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
        const val MIN_SAVE_MILLIS = 450L
    }
}
