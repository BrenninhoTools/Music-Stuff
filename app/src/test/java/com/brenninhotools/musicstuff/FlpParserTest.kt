package com.brenninhotools.musicstuff

import com.brenninhotools.musicstuff.FlpFixture.Companion.clip
import com.brenninhotools.musicstuff.FlpFixture.Companion.note
import com.brenninhotools.musicstuff.flp.FlpChannelKind
import com.brenninhotools.musicstuff.flp.FlpFormatException
import com.brenninhotools.musicstuff.flp.FlpParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FlpParserTest {
    private fun base() = FlpFixture().version("21.2.3.4000")

    @Test
    fun readsProjectFields() {
        val project = FlpParser.parse(
            base().text(194, "Música").dword(156, 128500).byte(17, 3).byte(18, 8).build(),
        )
        assertEquals("Música", project.title)
        assertEquals(128.5, project.tempo, 0.0001)
        assertEquals(3, project.timeSigNumerator)
        assertEquals(8, project.timeSigDenominator)
        assertEquals("21.2.3.4000", project.version)
    }

    @Test
    fun legacyVersionsUseSingleByteText() {
        val project = FlpParser.parse(
            FlpFixture().version("9.1.0").data(194, "Old".toByteArray(Charsets.ISO_8859_1)).build(),
        )
        assertEquals("Old", project.title)
    }

    @Test
    fun combinesCoarseAndFineTempo() {
        val project = FlpParser.parse(base().word(66, 120).word(93, 500).build())
        assertEquals(120.5, project.tempo, 0.0001)
    }

    @Test
    fun channelNamePrefersDisplayNameOverPlugin() {
        val project = FlpParser.parse(
            base()
                .word(64, 0).byte(21, 2).text(201, "Sytrus").text(203, "Lead")
                .word(64, 1).byte(21, 2).text(201, "FLEX")
                .word(64, 2).byte(21, 5)
                .build(),
        )
        assertEquals(listOf("Lead", "FLEX", "Channel 3"), project.channels.map { it.name })
        assertEquals(FlpChannelKind.AUTOMATION, project.channels[2].kind)
    }

    @Test
    fun mixerPluginNamesDoNotRenameTheLastChannel() {
        val project = FlpParser.parse(
            base()
                .word(64, 0).text(203, "Kick")
                .data(225, ByteArray(12))
                .text(203, "Fruity Limiter")
                .build(),
        )
        assertEquals("Kick", project.channels.single().name)
    }

    @Test
    fun patternsAreKeyedByTheirOneBasedId() {
        val project = FlpParser.parse(
            base()
                .word(65, 1).text(193, "Intro")
                .notes(note(0, 24, 60, channel = 3, velocity = 90, release = 50, slide = true))
                .word(65, 4)
                .notes(note(96, 48, 64))
                .build(),
        )
        assertEquals(listOf(1, 4), project.patterns.map { it.iid })
        assertEquals("Intro", project.patterns[0].name)
        assertEquals("Pattern 4", project.patterns[1].name)
        val first = project.patterns[0].notes.single()
        assertEquals(60, first.key)
        assertEquals(3, first.channelIid)
        assertEquals(90, first.velocity)
        assertEquals(50, first.release)
        assertTrue(first.slide)
        assertFalse(project.patterns[1].notes.single().slide)
    }

    @Test
    fun readsPlaylistItemsForOldAndNewSizes() {
        val old = FlpParser.parse(
            FlpFixture().version("20.8.4").word(99, 0).data(233, clip(0, 1, 96) + clip(96, 2, 96)).build(),
        )
        assertEquals(listOf(1, 2), old.arrangements.single().clips.map { it.patternIid })

        val items = (1..5).map { clip(it * 96, it, 96, itemSize = 60) }.reduce(ByteArray::plus)
        val new = FlpParser.parse(FlpFixture().version("24.1.1").word(99, 0).data(233, items).build())
        assertEquals(listOf(1, 2, 3, 4, 5), new.arrangements.single().clips.map { it.patternIid })
    }

    @Test
    fun channelClipsHaveNoPattern() {
        val project = FlpParser.parse(
            base().word(99, 0).data(233, FlpFixture.channelClip(0, 3, 96) + ByteArray(28)).build(),
        )
        assertNull(project.arrangements.single().clips.single().patternIid)
    }

    @Test
    fun rejectsFilesThatAreNotProjects() {
        assertThrows(FlpFormatException::class.java) { FlpParser.parse(ByteArray(64)) }
        assertThrows(FlpFormatException::class.java) { FlpParser.parse("PK\u0003\u0004 archive".toByteArray()) }
        assertThrows(FlpFormatException::class.java) { FlpParser.parse(ByteArray(3)) }
    }

    @Test
    fun keepsWhatItCanFromATruncatedFile() {
        val full = base().word(64, 0).text(203, "Kick").word(65, 1).notes(note(0, 24, 60)).build()
        val project = FlpParser.parse(full.copyOf(full.size - 10))
        assertEquals("Kick", project.channels.single().name)
        assertTrue(project.warnings.isNotEmpty())
    }

    @Test
    fun survivesEveryTruncationPoint() {
        val full = base().word(64, 0).text(203, "Kick").word(65, 1).notes(note(0, 24, 60)).build()
        for (size in 22..full.size) FlpParser.parse(full.copyOf(size))
    }
}
