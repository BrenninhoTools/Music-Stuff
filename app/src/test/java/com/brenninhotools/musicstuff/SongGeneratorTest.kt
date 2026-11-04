package com.brenninhotools.musicstuff

import com.brenninhotools.musicstuff.flm.FlmWriter
import com.brenninhotools.musicstuff.generate.PreviewSynth
import com.brenninhotools.musicstuff.generate.SongGenerator
import com.brenninhotools.musicstuff.generate.SongPart
import com.brenninhotools.musicstuff.generate.SongSettings
import com.brenninhotools.musicstuff.generate.SongStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongGeneratorTest {
    @Test
    fun sameSettingsMakeTheSameSong() {
        val settings = SongSettings(seed = 42)
        assertEquals(SongGenerator.generate(settings), SongGenerator.generate(settings))
    }

    @Test
    fun differentSeedsMakeDifferentSongs() {
        val first = SongGenerator.generate(SongSettings(seed = 1)).tracks.last().track
        val second = SongGenerator.generate(SongSettings(seed = 2)).tracks.last().track
        assertFalse(first == second)
    }

    @Test
    fun shufflingOnePartKeepsTheOthers() {
        val base = SongSettings(seed = 7)
        val shuffled = base.copy(partSeeds = mapOf(SongPart.MELODY to 99L))
        val before = SongGenerator.generate(base).tracks.associateBy { it.part }
        val after = SongGenerator.generate(shuffled).tracks.associateBy { it.part }
        assertFalse(before[SongPart.MELODY] == after[SongPart.MELODY])
        SongPart.entries.filter { it != SongPart.MELODY }.forEach { part ->
            assertEquals(before[part], after[part])
        }
    }

    @Test
    fun pitchedPartsStayInKeyForEveryStyle() {
        for (style in SongStyle.entries) for (root in 0..11) for (seed in 0L..5L) {
            val settings = SongSettings(style = style, root = root, bars = 32, seed = seed)
            val allowed = SongGenerator.scale(style.minor).map { (it + root) % 12 }.toSet()
            SongGenerator.generate(settings).tracks
                .filter { it.part in listOf(SongPart.BASS, SongPart.CHORDS, SongPart.MELODY) }
                .flatMap { it.track.notes }
                .forEach { assertTrue(it.key % 12 in allowed) }
        }
    }

    @Test
    fun notesStayInsideTheSongAndValidRanges() {
        for (style in SongStyle.entries) for (bars in listOf(8, 16, 32)) {
            val settings = SongSettings(style = style, bars = bars, seed = 3)
            val limit = bars * 4.0
            SongGenerator.generate(settings).tracks.flatMap { it.track.notes }.forEach {
                assertTrue(it.positionBeats < limit)
                assertTrue(it.lengthBeats > 0.0)
                assertTrue(it.key in 0..127)
                assertTrue(it.velocity in 1..127)
            }
        }
    }

    @Test
    fun everyPartHasNotes() {
        SongGenerator.generate(SongSettings(bars = 8, seed = 5)).tracks.forEach {
            assertTrue(it.track.notes.isNotEmpty())
        }
    }

    @Test
    fun generatedSongsWriteToFlm() {
        val song = SongGenerator.generate(SongSettings(root = 2, seed = 9))
        val reader = FlmReader(FlmWriter.write(song.project))
        assertEquals("Funkin' in D minor", reader.title())
        assertEquals(160.0, reader.tempo(), 0.0)
        assertEquals(listOf("MASTER", "Kick", "Snare", "Hats", "Bass", "Chords", "Melody"), reader.channels().map { it.name })
        assertEquals(song.tracks.sumOf { it.track.notes.size }, reader.channels().sumOf { it.notes.size })
    }

    @Test
    fun previewIsAudibleAndMatchesTheSongLength() {
        val song = SongGenerator.generate(SongSettings(bpm = 120, bars = 8, seed = 11))
        val audio = PreviewSynth.render(song)
        assertEquals(((16.0 + 0.5) * PreviewSynth.SAMPLE_RATE).toInt(), audio.size)
        assertTrue(audio.maxOf { it.toInt() } > 8000)
    }
}
