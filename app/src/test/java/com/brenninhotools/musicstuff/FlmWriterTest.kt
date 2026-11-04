package com.brenninhotools.musicstuff

import com.brenninhotools.musicstuff.flm.FlmNote
import com.brenninhotools.musicstuff.flm.FlmProject
import com.brenninhotools.musicstuff.flm.FlmTrack
import com.brenninhotools.musicstuff.flm.FlmWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlmWriterTest {
    private val note = FlmNote(positionBeats = 3.5, lengthBeats = 0.25, key = 72, velocity = 127, release = 64)

    private fun project(title: String = "Song", tracks: List<FlmTrack> = listOf(FlmTrack("Lead", listOf(note)))) =
        FlmProject(title, 174.25, 6, 8, tracks)

    @Test
    fun chunksFollowTheRealFileOrder() {
        val reader = FlmReader(FlmWriter.write(project()))
        assertEquals(listOf("HEAD", "KEYB", "META", "TDIV", "RACK", "RACK", "CHNL", "CHNL"), reader.chunks().map { it.tag })
    }

    @Test
    fun fixedSizeChunksMatchARealProject() {
        val data = FlmWriter.write(project())
        assertEquals("10LFHEAD", String(data, 0, 8, Charsets.ISO_8859_1))
        val chunks = FlmReader(data).chunks()
        val sizes = chunks.associate { it.tag to it.end - it.start }
        assertEquals(368, sizes["HEAD"])
        assertEquals(3081, sizes["META"])
        assertEquals(1102, sizes["KEYB"])
        assertEquals(2, sizes["TDIV"])
    }

    @Test
    fun storesProjectFields() {
        val reader = FlmReader(FlmWriter.write(project()))
        assertEquals("Song", reader.title())
        assertEquals(174.25, reader.tempo(), 0.0)
        assertEquals(6 to 8, reader.timeSignature())
    }

    @Test
    fun everyChannelHasARack() {
        val racks = FlmReader(FlmWriter.write(project())).racks()
        assertEquals(listOf("RHED", "RPRM", "RSMP"), racks[0])
        assertEquals(listOf("RHED", "RPRM", "RSMP", "RMOd"), racks[1])
    }

    @Test
    fun masterComesFirstAndNotesRoundTrip() {
        val channels = FlmReader(FlmWriter.write(project())).channels()
        assertEquals(listOf("MASTER", "Lead"), channels.map { it.name })
        assertTrue(channels[0].clips.isEmpty())
        val clip = channels[1].clips.single()
        assertEquals(0, clip.position)
        assertEquals(1, clip.id)
        assertEquals(3.75, clip.contentBeats, 0.0)
        assertEquals(4.0, clip.loopBeats, 0.0)
        val read = clip.notes.single()
        assertEquals(448, read.position)
        assertEquals(0.25, read.lengthBeats, 0.0)
        assertEquals(72, read.key)
        assertEquals(32767, read.velocity)
        assertEquals(32767, read.release)
    }

    @Test
    fun channelHeadersHaveTheirRealSizes() {
        val reader = FlmReader(FlmWriter.write(project(tracks = listOf(FlmTrack("BF", listOf(note))))))
        assertEquals(1102, reader.channelHeaderSizes()[0])
        assertEquals(1024 + 60 + 16 + 4 + 2 + 4, reader.channelHeaderSizes()[1])
    }

    @Test
    fun loopLengthGrowsInPowersOfTwo() {
        val long = note.copy(positionBeats = 40.0)
        val clip = FlmReader(FlmWriter.write(project(tracks = listOf(FlmTrack("Long", listOf(long)))))).channels()[1].clips.single()
        assertEquals(40.25, clip.contentBeats, 0.0)
        assertEquals(64.0, clip.loopBeats, 0.0)
    }

    @Test
    fun positionsBeyondOldLimitsStayExact() {
        val far = note.copy(positionBeats = 5000.0)
        val read = FlmReader(FlmWriter.write(project(tracks = listOf(FlmTrack("Far", listOf(far)))))).channels()[1].notes.single()
        assertEquals(640000, read.position)
    }

    @Test
    fun emptyChannelsHaveNoClip() {
        val channels = FlmReader(FlmWriter.write(project(tracks = listOf(FlmTrack("Empty", emptyList()))))).channels()
        assertEquals("Empty", channels[1].name)
        assertTrue(channels[1].clips.isEmpty())
    }

    @Test
    fun namesAreAlwaysPrintableAscii() {
        val data = FlmWriter.write(project(title = "Canção 東京", tracks = listOf(FlmTrack("Pé de ☃", listOf(note)))))
        val reader = FlmReader(data)
        assertEquals("Cancao __", reader.title())
        assertEquals("Pe de _", reader.channels()[1].name)
    }

    @Test
    fun longNamesStayInsideTheirFields() {
        val data = FlmWriter.write(project(title = "x".repeat(400), tracks = listOf(FlmTrack("y".repeat(400), listOf(note)))))
        val reader = FlmReader(data)
        assertEquals(255, reader.title().length)
        assertEquals(255, reader.channels()[1].name.length)
        assertEquals(368, reader.chunks().first().let { it.end - it.start })
    }
}
