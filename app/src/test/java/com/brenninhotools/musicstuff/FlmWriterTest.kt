package com.brenninhotools.musicstuff

import com.brenninhotools.musicstuff.flm.FlmNote
import com.brenninhotools.musicstuff.flm.FlmProject
import com.brenninhotools.musicstuff.flm.FlmTrack
import com.brenninhotools.musicstuff.flm.FlmWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlmWriterTest {
    private val note = FlmNote(positionBeats = 3.5, lengthBeats = 0.25, key = 72, velocity = 127, release = 64, slide = true)

    private fun project(title: String = "Song", tracks: List<FlmTrack> = listOf(FlmTrack("Lead", listOf(note)))) =
        FlmProject(title, 174.25, 6, 8, tracks)

    @Test
    fun everyChunkFitsExactlyInsideItsParent() {
        val reader = FlmReader(FlmWriter.write(project()))
        assertEquals(listOf("HEAD", "TDIV", "KEYB", "CHNL", "CHNL"), reader.chunks().map { it.tag })
    }

    @Test
    fun headIsTheSizeThatFlmFilesDeclare() {
        val data = FlmWriter.write(project())
        assertEquals("10LFHEAD", String(data, 0, 8, Charsets.ISO_8859_1))
        val head = FlmReader(data).chunks().first()
        assertEquals(369, head.end - head.start)
    }

    @Test
    fun storesProjectFields() {
        val reader = FlmReader(FlmWriter.write(project()))
        assertEquals("Song", reader.title())
        assertEquals(174.25, reader.tempo(), 0.0)
        assertEquals(6 to 8, reader.timeSignature())
    }

    @Test
    fun masterComesFirstAndNotesRoundTrip() {
        val channels = FlmReader(FlmWriter.write(project())).channels()
        assertEquals(listOf("MASTER", "Lead"), channels.map { it.name })
        assertTrue(channels[0].notes.isEmpty())
        val read = channels[1].notes.single()
        assertEquals(3.5, read.beats, 0.0)
        assertEquals(0.25f, read.lengthBeats, 0f)
        assertEquals(72, read.key)
        assertEquals(32767, read.velocity)
        assertTrue(read.slide)
    }

    @Test
    fun emptyChannelsHaveNoClip() {
        val channels = FlmReader(FlmWriter.write(project(tracks = listOf(FlmTrack("Empty", emptyList()))))).channels()
        assertEquals("Empty", channels[1].name)
        assertTrue(channels[1].notes.isEmpty())
    }

    @Test
    fun namesAreAlwaysPrintableAscii() {
        val data = FlmWriter.write(project(title = "Canção 東京", tracks = listOf(FlmTrack("Pé de ☃", emptyList()))))
        val reader = FlmReader(data)
        assertEquals("Cancao __", reader.title())
        assertEquals("Pe de _", reader.channels()[1].name)
    }

    @Test
    fun longNamesStayInsideTheirFields() {
        val data = FlmWriter.write(project(title = "x".repeat(400), tracks = listOf(FlmTrack("y".repeat(400), emptyList()))))
        val reader = FlmReader(data)
        assertEquals(255, reader.title().length)
        assertEquals(127, reader.channels()[1].name.length)
        val head = reader.chunks().first()
        assertEquals(369, head.end - head.start)
    }

    @Test
    fun positionsNeverWrapAround() {
        val far = note.copy(positionBeats = 5000.0)
        val read = FlmReader(FlmWriter.write(project(tracks = listOf(FlmTrack("Far", listOf(far)))))).channels()[1].notes.single()
        assertEquals(FlmWriter.MAX_POSITION_BEATS, read.beats, 0.001)
        assertFalse(read.beats < 1000)
    }
}
