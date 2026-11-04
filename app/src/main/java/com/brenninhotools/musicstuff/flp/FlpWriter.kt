package com.brenninhotools.musicstuff.flp

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration
import java.time.LocalDateTime
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
    private const val MILLIS_PER_DAY = 86_400_000.0

    private class Events {
        private val out = ByteArrayOutputStream()

        fun byte(id: Int, value: Int) = apply {
            out.write(id)
            out.write(value)
        }

        fun word(id: Int, value: Int) = apply {
            out.write(id)
            out.write(value and 0xFF)
            out.write((value shr 8) and 0xFF)
        }

        fun dword(id: Int, value: Int) = apply {
            out.write(id)
            out.write(little(4).putInt(value).array())
        }

        fun data(id: Int, value: ByteArray) = apply {
            out.write(id)
            var size = value.size
            while (size >= 0x80) {
                out.write((size and 0x7F) or 0x80)
                size = size shr 7
            }
            out.write(size)
            out.write(value)
        }

        fun text(id: Int, value: String) = data(id, (value + "\u0000").toByteArray(Charsets.UTF_16LE))

        fun raw(id: Int, value: ByteArray) = apply {
            out.write(id)
            out.write(value)
        }

        fun size(): Int = out.size()

        fun toByteArray(): ByteArray = out.toByteArray()
    }

    private fun little(size: Int): ByteBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

    private fun hex(value: String): ByteArray =
        ByteArray(value.length / 2) { value.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    fun write(project: FlpProject, savedAt: LocalDateTime = LocalDateTime.now()): ByteArray {
        val events = Events()
        projectEvents(events, project, savedAt)
        project.patterns.forEach { patternEvents(events, it) }
        FlpTemplate.PATTERN_CONTROLLERS.forEach { events.data(226, hex(it)) }
        project.channels.forEach { channelEvents(events, it) }
        arrangementEvents(events, project)
        val header = little(22)
            .put("FLhd".toByteArray(Charsets.US_ASCII))
            .putInt(6)
            .putShort(0)
            .putShort(project.channels.size.toShort())
            .putShort(project.ppq.toShort())
            .put("FLdt".toByteArray(Charsets.US_ASCII))
            .putInt(events.size())
            .array()
        return header + events.toByteArray()
    }

    private fun projectEvents(events: Events, project: FlpProject, savedAt: LocalDateTime) {
        events
            .data(FlpEvents.FL_VERSION, (VERSION + "\u0000").toByteArray(Charsets.US_ASCII))
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

    private fun timestamp(savedAt: LocalDateTime): ByteArray {
        val days = Duration.between(LocalDateTime.of(1899, 12, 30, 0, 0), savedAt).toMillis() / MILLIS_PER_DAY
        return little(16).putDouble(days).putDouble(0.0).array()
    }

    private fun patternEvents(events: Events, pattern: FlpPattern) {
        events.word(FlpEvents.PATTERN_NEW, pattern.iid)
        events.text(FlpEvents.PATTERN_NAME, pattern.name)
        if (pattern.notes.isNotEmpty()) {
            val notes = little(pattern.notes.size * 24)
            pattern.notes.forEach { note ->
                notes
                    .putInt(note.position.toInt())
                    .putShort((NOTE_FLAGS or if (note.slide) SLIDE_FLAG else 0).toShort())
                    .putShort(note.channelIid.toShort())
                    .putInt(note.length.toInt())
                    .putShort(note.key.toShort())
                    .putShort(0)
                    .put(FINE_PITCH.toByte())
                    .put(0)
                    .put(note.release.toByte())
                    .put(0)
                    .put(PAN_CENTER.toByte())
                    .put(note.velocity.toByte())
                    .put(MOD_CENTER.toByte())
                    .put(MOD_CENTER.toByte())
            }
            events.data(FlpEvents.PATTERN_NOTES, notes.array())
        }
    }

    private fun channelEvents(events: Events, channel: FlpChannel) {
        events
            .word(FlpEvents.CHANNEL_NEW, channel.iid)
            .byte(FlpEvents.CHANNEL_TYPE, FlpChannelKind.SAMPLER.code)
            .text(FlpEvents.PLUGIN_INTERNAL_NAME, "")
            .text(FlpEvents.PLUGIN_NAME, channel.name)
        FlpTemplate.CHANNEL_EVENTS.forEach { (id, value) ->
            val bytes = hex(value)
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
        val items = little(clips.size * PLAYLIST_ITEM_SIZE)
        clips.forEach { clip ->
            items
                .putInt(clip.position.toInt())
                .putShort(PATTERN_BASE.toShort())
                .putShort((PATTERN_BASE + (clip.patternIid ?: 0)).toShort())
                .putInt(clip.length.toInt())
                .putShort((MAX_TRACK_INDEX - clip.track).toShort())
                .putShort(0)
                .put(120)
                .put(0)
                .put(64)
                .put(0)
                .put(64)
                .put(100)
                .put(MOD_CENTER.toByte())
                .put(MOD_CENTER.toByte())
                .putInt(0)
                .putInt(clip.length.toInt())
        }
        events.data(FlpEvents.ARRANGEMENT_PLAYLIST, items.array())
        val tail = hex(FlpTemplate.TRACK_DATA_TAIL)
        for (track in 1..TRACK_COUNT) {
            events.data(238, little(4).putInt(track).array() + tail)
        }
    }
}
