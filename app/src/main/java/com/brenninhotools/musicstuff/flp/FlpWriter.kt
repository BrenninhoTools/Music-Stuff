package com.brenninhotools.musicstuff.flp

import com.brenninhotools.musicstuff.binary.ByteSink
import com.brenninhotools.musicstuff.binary.hexBytes
import com.brenninhotools.musicstuff.platform.DateStamp
import com.brenninhotools.musicstuff.platform.currentStamp
import kotlin.math.roundToLong

object FlpWriter {
    private const val VERSION = "20.8.4.2576"
    private const val BUILD = 2576
    private const val PATTERN_BASE = 20480
    private const val MAX_TRACK_INDEX = 499
    private const val TRACK_COUNT = 500
    private const val PLAYLIST_ITEM_SIZE = 32
    private const val NOTE_FLAGS = 0x4000
    private const val SLIDE_FLAG = 0x0008
    private const val FINE_PITCH = 120
    private const val PAN_CENTER = 64
    private const val MOD_CENTER = 128
    private const val DEFAULT_GROUP = "Unsorted"
    private const val ARRANGEMENT_NAME = "Arrangement"

    private class Events {
        private val out = ByteSink(1 shl 16)

        val size: Int get() = out.size

        fun byte(id: Int, value: Int) = apply {
            out.u8(id)
            out.u8(value)
        }

        fun word(id: Int, value: Int) = apply {
            out.u8(id)
            out.u16(value)
        }

        fun dword(id: Int, value: Int) = apply {
            out.u8(id)
            out.u32(value)
        }

        fun data(id: Int, value: ByteArray) = apply {
            out.u8(id)
            var remaining = value.size
            while (remaining >= 0x80) {
                out.u8((remaining and 0x7F) or 0x80)
                remaining = remaining shr 7
            }
            out.u8(remaining)
            out.bytes(value)
        }

        fun text(id: Int, value: String) = data(
            id,
            ByteSink().also { it.utf16le(value + "\u0000") }.toByteArray(),
        )

        fun raw(id: Int, value: ByteArray) = apply {
            out.u8(id)
            out.bytes(value)
        }

        fun toByteArray(): ByteArray = out.toByteArray()
    }

    fun write(project: FlpProject, savedAt: DateStamp = currentStamp()): ByteArray {
        val events = Events()
        projectEvents(events, project, savedAt)
        project.patterns.forEach { patternEvents(events, it) }
        FlpTemplate.PATTERN_CONTROLLERS.forEach { events.data(226, hexBytes(it)) }
        project.channels.forEach { channelEvents(events, it) }
        arrangementEvents(events, project)
        val body = events.toByteArray()
        val file = ByteSink(body.size + 22)
        file.latin1("FLhd")
        file.u32(6)
        file.u16(0)
        file.u16(project.channels.size)
        file.u16(project.ppq)
        file.latin1("FLdt")
        file.u32(events.size)
        file.bytes(body)
        return file.toByteArray()
    }

    private fun projectEvents(events: Events, project: FlpProject, savedAt: DateStamp) {
        events
            .data(FlpEvents.FL_VERSION, (VERSION + "\u0000").encodeToByteArray())
            .dword(159, BUILD)
            .byte(28, 1)
            .byte(37, 1)
            .dword(FlpEvents.TEMPO, (project.tempo * 1000).roundToLong().toInt())
            .word(67, project.patterns.firstOrNull()?.iid ?: 1)
            .byte(9, 0)
            .byte(11, 0)
            .word(80, 0)
            .byte(FlpEvents.TIME_SIG_NUMERATOR, project.timeSigNumerator)
            .byte(FlpEvents.TIME_SIG_DENOMINATOR, project.timeSigDenominator)
            .byte(35, 1)
            .byte(23, 0)
            .byte(30, 1)
            .byte(10, 0)
            .text(FlpEvents.TITLE, project.title)
            .text(206, "")
            .text(207, "")
            .text(202, "")
            .text(195, "")
            .data(237, timestamp(savedAt))
            .text(231, DEFAULT_GROUP)
            .dword(146, -1)
            .data(216, ByteArray(0))
    }

    private fun timestamp(savedAt: DateStamp): ByteArray {
        val sink = ByteSink(16)
        sink.f64(savedAt.delphiDays())
        sink.f64(0.0)
        return sink.toByteArray()
    }

    private fun patternEvents(events: Events, pattern: FlpPattern) {
        events.word(FlpEvents.PATTERN_NEW, pattern.iid)
        events.text(FlpEvents.PATTERN_NAME, pattern.name)
        if (pattern.notes.isNotEmpty()) {
            val notes = ByteSink(pattern.notes.size * 24)
            pattern.notes.forEach { note ->
                notes.u32(note.position.toInt())
                notes.u16(NOTE_FLAGS or if (note.slide) SLIDE_FLAG else 0)
                notes.u16(note.channelIid)
                notes.u32(note.length.toInt())
                notes.u16(note.key)
                notes.u16(0)
                notes.u8(FINE_PITCH)
                notes.u8(0)
                notes.u8(note.release)
                notes.u8(0)
                notes.u8(PAN_CENTER)
                notes.u8(note.velocity)
                notes.u8(MOD_CENTER)
                notes.u8(MOD_CENTER)
            }
            events.data(FlpEvents.PATTERN_NOTES, notes.toByteArray())
        }
    }

    private fun channelEvents(events: Events, channel: FlpChannel) {
        events
            .word(FlpEvents.CHANNEL_NEW, channel.iid)
            .byte(FlpEvents.CHANNEL_TYPE, FlpChannelKind.SAMPLER.code)
            .text(FlpEvents.PLUGIN_INTERNAL_NAME, "")
            .text(FlpEvents.PLUGIN_NAME, channel.name)
        FlpTemplate.CHANNEL_EVENTS.forEach { (id, value) ->
            val bytes = hexBytes(value)
            if (id < FlpEvents.TEXT_START) events.raw(id, bytes) else events.data(id, bytes)
        }
    }

    private fun arrangementEvents(events: Events, project: FlpProject) {
        val arrangement = project.arrangements.firstOrNull()
        events
            .word(FlpEvents.ARRANGEMENT_NEW, 0)
            .text(FlpEvents.ARRANGEMENT_NAME, arrangement?.name ?: ARRANGEMENT_NAME)
            .byte(36, 0)
        val clips = arrangement?.clips.orEmpty().filter { it.patternIid != null }.sortedBy { it.position }
        val items = ByteSink(clips.size.coerceAtLeast(1) * PLAYLIST_ITEM_SIZE)
        clips.forEach { clip ->
            items.u32(clip.position.toInt())
            items.u16(PATTERN_BASE)
            items.u16(PATTERN_BASE + (clip.patternIid ?: 0))
            items.u32(clip.length.toInt())
            items.u16(MAX_TRACK_INDEX - clip.track)
            items.u16(0)
            items.u16(120)
            items.u16(64)
            items.u8(64)
            items.u8(100)
            items.u8(MOD_CENTER)
            items.u8(MOD_CENTER)
            items.u32(0)
            items.u32(clip.length.toInt())
        }
        events.data(FlpEvents.ARRANGEMENT_PLAYLIST, items.toByteArray())
        val tail = hexBytes(FlpTemplate.TRACK_DATA_TAIL)
        for (track in 1..TRACK_COUNT) {
            val data = ByteSink(tail.size + 4)
            data.u32(track)
            data.bytes(tail)
            events.data(238, data.toByteArray())
        }
    }
}
