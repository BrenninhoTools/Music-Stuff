package com.brenninhotools.musicstuff.generate

import com.brenninhotools.musicstuff.binary.ByteSink
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

object PreviewSynth {
    const val SAMPLE_RATE = 44100

    private const val TAIL_SECONDS = 0.5
    private const val ATTACK_SECONDS = 0.004
    private const val RELEASE_SECONDS = 0.05
    private const val MAX_SAMPLE = 32000f
    private const val DRIVE = 1.1f
    private const val WAV_HEADER_SIZE = 44

    fun render(song: GeneratedSong): ShortArray {
        val secondsPerBeat = 60.0 / song.settings.bpm
        val seconds = SongGenerator.endBeats(song.settings) * secondsPerBeat + TAIL_SECONDS
        val mix = FloatArray((seconds * SAMPLE_RATE).toInt())
        val noise = Random(song.settings.seed)
        song.tracks.forEach { generated ->
            generated.track.notes.forEach { note ->
                val start = (note.positionBeats * secondsPerBeat * SAMPLE_RATE).toInt()
                val length = note.lengthBeats * secondsPerBeat
                val level = note.velocity / 127f
                when (generated.part) {
                    SongPart.KICK -> kick(mix, start, level)
                    SongPart.SNARE -> snare(mix, start, level, noise)
                    SongPart.HATS -> hat(mix, start, level, noise)
                    SongPart.BASS -> bass(mix, start, length, note.key, level)
                    SongPart.CHORDS -> pad(mix, start, length, note.key, level)
                    SongPart.MELODY -> lead(mix, start, length, note.key, level)
                }
            }
        }
        return ShortArray(mix.size) { (tanh(mix[it] * DRIVE) * MAX_SAMPLE).toInt().toShort() }
    }

    fun wav(samples: ShortArray): ByteArray {
        val sink = ByteSink(samples.size * 2 + WAV_HEADER_SIZE)
        val dataSize = samples.size * 2
        sink.latin1("RIFF")
        sink.u32(dataSize + WAV_HEADER_SIZE - 8)
        sink.latin1("WAVEfmt ")
        sink.u32(16)
        sink.u16(1)
        sink.u16(1)
        sink.u32(SAMPLE_RATE)
        sink.u32(SAMPLE_RATE * 2)
        sink.u16(2)
        sink.u16(16)
        sink.latin1("data")
        sink.u32(dataSize)
        samples.forEach { sink.u16(it.toInt()) }
        return sink.toByteArray()
    }

    private fun frequency(key: Int): Double = 440.0 * 2.0.pow((key - 69) / 12.0)

    private inline fun write(mix: FloatArray, start: Int, seconds: Double, sample: (Double, Int) -> Float) {
        val count = (seconds * SAMPLE_RATE).toInt()
        for (index in 0 until count) {
            val target = start + index
            if (target >= mix.size) return
            mix[target] += sample(index.toDouble() / SAMPLE_RATE, index)
        }
    }

    private fun envelope(time: Double, length: Double): Float {
        val attack = (time / ATTACK_SECONDS).coerceAtMost(1.0)
        val release = ((length - time) / RELEASE_SECONDS).coerceIn(0.0, 1.0)
        return (attack * release).toFloat()
    }

    private fun kick(mix: FloatArray, start: Int, level: Float) {
        var phase = 0.0
        write(mix, start, 0.28) { time, _ ->
            phase += 2 * PI * (48.0 + 110.0 * exp(-time * 34.0)) / SAMPLE_RATE
            (sin(phase) * exp(-time * 11.0)).toFloat() * level * 0.8f
        }
    }

    private fun snare(mix: FloatArray, start: Int, level: Float, noise: Random) {
        write(mix, start, 0.2) { time, _ ->
            val tone = sin(2 * PI * 190.0 * time) * exp(-time * 26.0) * 0.4
            val hiss = (noise.nextDouble() * 2 - 1) * exp(-time * 18.0) * 0.5
            (tone + hiss).toFloat() * level * 0.6f
        }
    }

    private fun hat(mix: FloatArray, start: Int, level: Float, noise: Random) {
        var previous = 0.0
        write(mix, start, 0.05) { time, _ ->
            val current = noise.nextDouble() * 2 - 1
            val bright = current - previous
            previous = current
            (bright * exp(-time * 90.0)).toFloat() * level * 0.11f
        }
    }

    private fun bass(mix: FloatArray, start: Int, length: Double, key: Int, level: Float) {
        val hertz = frequency(key)
        write(mix, start, length) { time, _ ->
            val tone = sin(2 * PI * hertz * time) + 0.35 * sin(4 * PI * hertz * time)
            tone.toFloat() * envelope(time, length) * level * 0.36f
        }
    }

    private fun pad(mix: FloatArray, start: Int, length: Double, key: Int, level: Float) {
        val hertz = frequency(key)
        write(mix, start, length) { time, _ ->
            val position = (hertz * time) % 1.0
            val triangle = 4 * abs(position - 0.5) - 1
            triangle.toFloat() * envelope(time, length) * level * 0.13f
        }
    }

    private fun lead(mix: FloatArray, start: Int, length: Double, key: Int, level: Float) {
        val hertz = frequency(key)
        write(mix, start, length) { time, _ ->
            val square = if ((hertz * time) % 1.0 < 0.5) 1f else -1f
            square * envelope(time, length) * level * 0.17f
        }
    }
}
