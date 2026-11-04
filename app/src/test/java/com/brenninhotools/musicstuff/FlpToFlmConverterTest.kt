package com.brenninhotools.musicstuff

import com.brenninhotools.musicstuff.FlpFixture.Companion.clip
import com.brenninhotools.musicstuff.FlpFixture.Companion.note
import com.brenninhotools.musicstuff.convert.FlmLayout
import com.brenninhotools.musicstuff.convert.FlpToFlmConverter
import com.brenninhotools.musicstuff.flm.FlmWriter
import com.brenninhotools.musicstuff.flp.FlpParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlpToFlmConverterTest {
    private fun channels() = FlpFixture().version("21.0.0")
        .word(64, 0).byte(21, 2).text(203, "Kick")
        .word(64, 1).byte(21, 2).text(203, "Bass")

    private fun convert(fixture: FlpFixture) = FlpToFlmConverter.convert(FlpParser.parse(fixture.build()))

    @Test
    fun followsThePlaylist() {
        val result = convert(
            channels()
                .word(65, 1).notes(note(0, 48, 60), note(48, 48, 62, channel = 1))
                .word(99, 0).data(233, clip(192, 1, 96, itemSize = 60) + clip(960, 1, 96, itemSize = 60)),
        )
        assertEquals(FlmLayout.ARRANGEMENT, result.layout)
        assertEquals(4, result.noteCount)
        val kick = result.project.tracks[0].notes
        assertEquals(listOf(2.0, 10.0), kick.map { it.positionBeats })
        assertEquals(listOf(2.5, 10.5), result.project.tracks[1].notes.map { it.positionBeats })
    }

    @Test
    fun clipsTrimNotesToTheirWindow() {
        val result = convert(
            channels()
                .word(65, 1).notes(note(0, 24, 60), note(48, 96, 62), note(192, 24, 64))
                .word(99, 0).data(233, clip(0, 1, 96, startOffset = 24f, itemSize = 60)),
        )
        val notes = result.project.tracks[0].notes
        assertEquals(1, notes.size)
        assertEquals(0.25, notes[0].positionBeats, 0.0001)
        assertEquals(0.75, notes[0].lengthBeats, 0.0001)
    }

    @Test
    fun missingClipOffsetsStartAtZero() {
        val result = convert(
            channels()
                .word(65, 1).notes(note(0, 24, 60))
                .word(99, 0).data(233, clip(96, 1, 96, startOffset = Float.NaN, itemSize = 60)),
        )
        assertEquals(1.0, result.project.tracks[0].notes.single().positionBeats, 0.0001)
    }

    @Test
    fun chainsPatternsOnWholeBarsWithoutAPlaylist() {
        val result = convert(
            channels()
                .word(65, 1).notes(note(0, 96, 60))
                .word(65, 2).notes(note(0, 480, 62))
                .word(65, 3).notes(note(0, 96, 64)),
        )
        assertEquals(FlmLayout.SEQUENTIAL, result.layout)
        assertEquals(listOf(0.0, 4.0, 12.0), result.project.tracks[0].notes.map { it.positionBeats })
    }

    @Test
    fun skipsAudioClipsAndAutomationChannels() {
        val result = convert(
            channels()
                .word(64, 2).byte(21, 5).text(203, "Filter")
                .word(65, 1).notes(note(0, 96, 60))
                .word(99, 0).data(233, clip(0, 1, 96) + FlpFixture.channelClip(0, 1, 96)),
        )
        assertEquals(listOf("Kick", "Bass"), result.project.tracks.map { it.name })
        assertTrue(result.warnings.any { it.contains("1 audio or automation clip") })
    }

    @Test
    fun reportsNotesPastThePositionLimit() {
        val result = convert(
            channels()
                .word(65, 1).notes(note(0, 96, 60), note(96 * 1024, 96, 60)),
        )
        assertEquals(1, result.noteCount)
        assertTrue(result.warnings.any { it.contains("start after beat 1023") })
    }

    @Test
    fun movesKeysAboveTheLimitDownAnOctave() {
        val result = convert(channels().word(65, 1).notes(note(0, 96, 130)))
        assertEquals(118, result.project.tracks[0].notes.single().key)
        assertTrue(result.warnings.any { it.contains("moved down an octave") })
    }

    @Test
    fun timingDoesNotDependOnPpq() {
        val fixture = FlpFixture(ppq = 960).version("21.0.0")
            .word(64, 0).text(203, "Keys")
            .word(65, 1).notes(note(1920, 480, 60))
        val note = convert(fixture).project.tracks.single().notes.single()
        assertEquals(2.0, note.positionBeats, 0.0001)
        assertEquals(0.5, note.lengthBeats, 0.0001)
    }

    @Test
    fun aChannelCalledMasterDoesNotCollide() {
        val result = convert(FlpFixture().version("21.0.0").word(64, 0).text(203, "Master"))
        assertEquals("Master 2", result.project.tracks.single().name)
        assertEquals(listOf(FlmWriter.MASTER_NAME, "Master 2"), FlmReader(FlmWriter.write(result.project)).channels().map { it.name })
    }
}
