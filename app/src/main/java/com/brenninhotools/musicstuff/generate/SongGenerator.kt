package com.brenninhotools.musicstuff.generate

import com.brenninhotools.musicstuff.flm.FlmNote
import com.brenninhotools.musicstuff.flm.FlmProject
import com.brenninhotools.musicstuff.flm.FlmTrack
import kotlin.math.abs
import kotlin.random.Random

data class GeneratedTrack(val part: SongPart, val track: FlmTrack)

data class GeneratedSong(
    val settings: SongSettings,
    val tracks: List<GeneratedTrack>,
) {
    val project: FlmProject
        get() = FlmProject(
            title = settings.title,
            tempo = settings.bpm.toDouble(),
            timeSigNumerator = 4,
            timeSigDenominator = 4,
            tracks = tracks.map { it.track },
        )
}

object SongGenerator {
    private const val STEPS_PER_BAR = 16
    private const val BEATS_PER_STEP = 0.25
    private const val BEATS_PER_BAR = 4
    private const val PHRASE_BARS = 8
    private const val MOTIF_BARS = 2
    private const val OCTAVE = 12
    private const val DRUM_KEY = 60
    private const val BASS_BASE = 36
    private const val CHORD_BASE = 60
    private const val MELODY_BASE = 72
    private const val MELODY_LOW = -2
    private const val MELODY_HIGH = 9
    private const val RELEASE = 64
    private const val SEED_MIX = 0x9E3779B97F4A7C15uL

    private val MAJOR = listOf(0, 2, 4, 5, 7, 9, 11)
    private val MINOR = listOf(0, 2, 3, 5, 7, 8, 10)

    private data class MotifNote(val step: Int, val degree: Int, val steps: Int)

    fun generate(settings: SongSettings): GeneratedSong {
        val progression = settings.style.progressions[random(settings, null).nextInt(settings.style.progressions.size)]
        val tracks = SongPart.entries.map { part ->
            val notes = when (part) {
                SongPart.KICK -> drums(settings, settings.style.kick, 118, fill = false, part)
                SongPart.SNARE -> drums(settings, settings.style.snare, 112, fill = true, part)
                SongPart.HATS -> drums(settings, settings.style.hats, 78, fill = false, part)
                SongPart.BASS -> bass(settings, progression)
                SongPart.CHORDS -> chords(settings, progression)
                SongPart.MELODY -> melody(settings, progression)
            }
            GeneratedTrack(part, FlmTrack(part.label, notes))
        }
        return GeneratedSong(settings, tracks)
    }

    fun scale(minor: Boolean): List<Int> = if (minor) MINOR else MAJOR

    private fun random(settings: SongSettings, part: SongPart?): Random {
        val partSeed = part?.let { settings.partSeeds[it] ?: 0L } ?: 0L
        val salt = ((part?.ordinal ?: -1) + 2).toULong() * SEED_MIX
        return Random(settings.seed xor salt.toLong() xor partSeed)
    }

    private fun pitch(settings: SongSettings, base: Int, degree: Int): Int {
        val notes = scale(settings.style.minor)
        val octaves = degree.floorDiv(notes.size)
        return base + settings.root + notes[degree.mod(notes.size)] + octaves * OCTAVE
    }

    private fun note(step: Int, steps: Int, key: Int, velocity: Int) = FlmNote(
        positionBeats = step * BEATS_PER_STEP,
        lengthBeats = steps * BEATS_PER_STEP,
        key = key,
        velocity = velocity.coerceIn(1, 127),
        release = RELEASE,
    )

    private fun drums(settings: SongSettings, pattern: String, velocity: Int, fill: Boolean, part: SongPart): List<FlmNote> {
        val random = random(settings, part)
        val notes = mutableListOf<FlmNote>()
        for (bar in 0 until settings.bars) {
            pattern.forEachIndexed { step, cell ->
                if (cell == 'x') {
                    notes += note(bar * STEPS_PER_BAR + step, 1, DRUM_KEY, velocity + random.nextInt(-8, 9))
                }
            }
            if (fill && bar % PHRASE_BARS == PHRASE_BARS - 1) {
                listOf(13, 14, 15).filter { pattern[it] != 'x' }.forEach { step ->
                    notes += note(bar * STEPS_PER_BAR + step, 1, DRUM_KEY, velocity - 16)
                }
            }
        }
        return notes
    }

    private fun bass(settings: SongSettings, progression: List<Int>): List<FlmNote> {
        val random = random(settings, SongPart.BASS)
        val pattern = settings.style.bass
        val notes = mutableListOf<FlmNote>()
        val fifthChance = 0.2
        for (bar in 0 until settings.bars) {
            val degree = progression[bar % progression.size]
            val hits = pattern.indices.filter { pattern[it] == 'x' }
            hits.forEachIndexed { index, step ->
                val next = hits.getOrNull(index + 1) ?: STEPS_PER_BAR
                val offset = if (index > 0 && random.nextDouble() < fifthChance) 4 else 0
                notes += note(bar * STEPS_PER_BAR + step, next - step, pitch(settings, BASS_BASE, degree + offset), 104)
            }
        }
        return notes
    }

    private fun chords(settings: SongSettings, progression: List<Int>): List<FlmNote> {
        val random = random(settings, SongPart.CHORDS)
        val intervals = if (settings.style.sevenths) listOf(0, 2, 4, 6) else listOf(0, 2, 4)
        val notes = mutableListOf<FlmNote>()
        val offbeats = random.nextBoolean()
        for (bar in 0 until settings.bars) {
            val degree = progression[bar % progression.size]
            val hits = if (offbeats) listOf(0 to 6, 6 to 6, 12 to 4) else listOf(0 to STEPS_PER_BAR)
            hits.forEach { (step, steps) ->
                intervals.forEach { interval ->
                    notes += note(bar * STEPS_PER_BAR + step, steps, pitch(settings, CHORD_BASE, degree + interval), 76)
                }
            }
        }
        return notes
    }

    private fun motif(settings: SongSettings, random: Random, progression: List<Int>, startBar: Int): List<MotifNote> {
        val notes = mutableListOf<MotifNote>()
        val total = STEPS_PER_BAR * MOTIF_BARS
        var degree = progression[startBar % progression.size] % 7
        var step = 0
        while (step < total) {
            val onBeat = step % 4 == 0
            val chance = if (onBeat) settings.style.density + 0.2 else settings.style.density
            val steps = listOf(1, 2, 2, 3, 4).random(random)
            if (random.nextDouble() < chance) {
                val bar = startBar + step / STEPS_PER_BAR
                val chordRoot = progression[bar % progression.size]
                degree = if (onBeat) {
                    val tones = listOf(chordRoot, chordRoot + 2, chordRoot + 4)
                    tones.minByOrNull { abs(it - degree) } ?: chordRoot
                } else {
                    degree + listOf(-2, -1, -1, 1, 1, 2).random(random)
                }
                degree = degree.coerceIn(MELODY_LOW, MELODY_HIGH)
                notes += MotifNote(step, degree, steps)
            }
            step += steps
        }
        return notes
    }

    private fun melody(settings: SongSettings, progression: List<Int>): List<FlmNote> {
        val random = random(settings, SongPart.MELODY)
        val motifA = motif(settings, random, progression, 0)
        val motifB = motif(settings, random, progression, 4)
        val notes = mutableListOf<FlmNote>()
        for (bar in 0 until settings.bars step MOTIF_BARS) {
            val motif = when (bar / MOTIF_BARS % 4) {
                2 -> motifB
                else -> motifA
            }
            val closes = bar / MOTIF_BARS % 4 == 3
            motif.forEachIndexed { index, item ->
                val degree = if (closes && index == motif.lastIndex) 0 else item.degree
                val step = bar * STEPS_PER_BAR + item.step
                val velocity = if (item.step % 4 == 0) 104 else 90
                notes += note(step, item.steps, pitch(settings, MELODY_BASE, degree), velocity)
            }
        }
        return notes
    }

    fun endBeats(settings: SongSettings): Double = (settings.bars * BEATS_PER_BAR).toDouble()
}
