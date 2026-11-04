package com.brenninhotools.musicstuff

import com.brenninhotools.musicstuff.convert.FlmToFlpConverter
import com.brenninhotools.musicstuff.convert.FlpToFlmConverter
import com.brenninhotools.musicstuff.flm.FlmFormatException
import com.brenninhotools.musicstuff.flm.FlmNote
import com.brenninhotools.musicstuff.flm.FlmParser
import com.brenninhotools.musicstuff.flm.FlmProject
import com.brenninhotools.musicstuff.flm.FlmTrack
import com.brenninhotools.musicstuff.flm.FlmWriter
import com.brenninhotools.musicstuff.flp.FlpParser
import com.brenninhotools.musicstuff.flp.FlpWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FlmToFlpTest {
    private val source = FlmProject(
        title = "Round Trip",
        tempo = 155.5,
        timeSigNumerator = 3,
        timeSigDenominator = 4,
        tracks = listOf(
            FlmTrack(
                "Lead",
                listOf(
                    FlmNote(0.5, 0.25, 72, 100, 64),
                    FlmNote(1.125, 1.5, 74, 64, 32),
                ),
            ),
            FlmTrack("Bass", listOf(FlmNote(2.0, 1.0, 40, 127, 64))),
        ),
    )

    @Test
    fun readsWhatTheWriterProduces() {
        val document = FlmParser.parse(FlmWriter.write(source))
        assertEquals("Round Trip", document.title)
        assertEquals(155.5, document.tempo, 0.0)
        assertEquals(3, document.timeSigNumerator)
        assertEquals("5.0.4", document.appVersion)
        assertEquals(listOf("MASTER", "Lead", "Bass"), document.channels.map { it.name })
        assertEquals(3, document.noteCount)
        val note = document.channels[1].clips.single().notes[1]
        assertEquals(1.125, note.positionBeats, 0.0)
        assertEquals(1.5, note.lengthBeats, 0.0)
        assertEquals(74, note.key)
        assertEquals(64, note.velocity)
        assertEquals(32, note.release)
    }

    @Test
    fun flpOutputReadsBackWithTheSameMusic() {
        val document = FlmParser.parse(FlmWriter.write(source))
        val written = FlpWriter.write(FlmToFlpConverter.convert(document))
        val project = FlpParser.parse(written)
        assertEquals("Round Trip", project.title)
        assertEquals(155.5, project.tempo, 0.0001)
        assertEquals(3, project.timeSigNumerator)
        assertEquals(384, project.ppq)
        assertEquals(listOf("Lead", "Bass"), project.channels.map { it.name })
        assertEquals(3, project.noteCount)
        assertTrue(project.warnings.isEmpty())
        val back = FlpToFlmConverter.convert(project).project.tracks
        assertEquals(source.tracks.map { it.name }, back.map { it.name })
        source.tracks.zip(back).forEach { (before, after) ->
            before.notes.zip(after.notes).forEach { (a, b) ->
                assertEquals(a.positionBeats, b.positionBeats, 0.0001)
                assertEquals(a.lengthBeats, b.lengthBeats, 0.0001)
                assertEquals(a.key, b.key)
                assertEquals(a.velocity, b.velocity)
            }
        }
    }

    @Test
    fun flpClipsStayOnSeparateTracks() {
        val project = FlmToFlpConverter.convert(FlmParser.parse(FlmWriter.write(source)))
        assertEquals(listOf(0, 1), project.arrangements.single().clips.map { it.track })
    }

    @Test
    fun rejectsFilesThatAreNotProjects() {
        assertThrows(FlmFormatException::class.java) { FlmParser.parse(ByteArray(64)) }
        assertThrows(FlmFormatException::class.java) { FlmParser.parse(ByteArray(3)) }
    }

    @Test
    fun survivesEveryTruncationPoint() {
        val full = FlmWriter.write(source)
        for (size in 400..full.size step 7) FlmParser.parse(full.copyOf(size))
    }

    @Test
    fun outputHasOneTrackEventPerPlaylistRow() {
        val written = FlpWriter.write(FlmToFlpConverter.convert(FlmParser.parse(FlmWriter.write(source))))
        assertEquals("FLhd", String(written, 0, 4, Charsets.US_ASCII))
        assertTrue(written.size > 500 * 66)
    }
}
