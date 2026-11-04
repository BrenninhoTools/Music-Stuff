package com.brenninhotools.musicstuff.flm

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object FlmWriter {
    const val TICKS_PER_BEAT = 128
    const val MAX_KEY = 127
    const val MAX_LEVEL = 127
    const val MASTER_NAME = "MASTER"

    private const val MAGIC = "10LF"
    private const val FILE_VERSION = 3
    private const val APP_VERSION = "5.0.4"
    private const val RACK_MAGIC = "10KR"
    private const val CHANNEL_MAGIC = "20HC"
    private const val CLIP_MAGIC = "20LC"
    private const val CUSTOM_MAGIC = "10WD"
    private const val FLAGS_MASTER = 0
    private const val FLAGS_CHANNEL = 0x100
    private const val CHANNEL_NAME_SIZE = 1024
    private const val TEXT_FIELD_SIZE = 256
    private const val NAME_MAX_LENGTH = 255
    private const val NOTE_RECORD_SIZE = 20
    private const val LEVEL_MAX = 32767
    private const val DEFAULT_RELEASE = 64
    private const val MIN_CLIP_BEATS = 4.0
    private const val SAMPLER_NAME = "DW Sampler 1"
    private const val SAMPLER_MODULE_TYPE = 1
    private const val SAMPLER_MODULE_ID = 2
    private const val DEFAULT_PRESET_FOLDER = "Default"
    private const val DEFAULT_PRESET = "Default/Default"
    private const val META_SIZE = 3081
    private const val META_TITLE_OFFSET = 520
    private const val META_SAVED_OFFSET = 2825
    private const val MIN_LENGTH_BEATS = 0.001
    private const val ROW_VOLUME_BITS = 0x3F5A6657
    private const val PAN_CENTER = 0.5f

    private val CREATED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss")
    private val SAVED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val SAMPLER_GUID = hex("c9908dc3cafc218f124abfdabd018b6f")
    private val MASTER_TAIL = hex(
        "000000000000000000000000" +
            "5060653e000080f9b6a0e13f0000000000003c4000000000" +
            "00000000d6081954dfb3a772c5295a05706634e400000000",
    )
    private val CHANNEL_LEVELS = hex("0000803f000080f9b6a0e13f000000600d515cbf00000000")
    private val SEND_LEVELS = hex("000000000000803f0000803f0000803f57665a3f57665a3f")
    private val PADS_MAP = hex("31304d53000000005041445316000000000102030405060708090a0b0c0d0e0f101112131415")
    private val SAMPLER_PARAMS = hex(
        "0000803f000000000000803e0000003f0000003f0000803f0000803f00000000" +
            "000000000000803f00000000000000000000803f000000000000000000000000" +
            "00000000000000000000803f0000803f0000803f0000003f000000000000803f" +
            "0000803f0000803f0000003f000000000000000000000000000000000000003f" +
            "0000000000000000",
    )
    private val SAMPLE_RANGE = hex("0000003f0000803f0000003f")
    private val VIEW_TAIL = hex("00000080b4af5abf0000006059170b40000000c034ecd03f")

    private val CHORDS = listOf(
        Triple(0, 3, 0), Triple(2, 3, 1), Triple(4, 3, 1), Triple(5, 3, 0),
        Triple(7, 3, 0), Triple(9, 3, 1), Triple(11, 3, 3), Triple(0, 4, 0),
    )

    private fun hex(value: String): ByteArray =
        ByteArray(value.length / 2) { value.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private class Payload {
        private val out = ByteArrayOutputStream()

        fun u8(value: Int) = apply { out.write(value and 0xFF) }

        fun u16(value: Int) = apply {
            u8(value)
            u8(value shr 8)
        }

        fun u32(value: Int) = apply {
            u16(value)
            u16(value ushr 16)
        }

        fun f32(value: Float) = apply { u32(value.toRawBits()) }

        fun f64(value: Double) = apply {
            bytes(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array())
        }

        fun ascii(value: String) = apply { bytes(value.toByteArray(Charsets.ISO_8859_1)) }

        fun field(value: String, size: Int) = apply {
            val text = value.take(size - 1)
            ascii(text)
            zeros(size - text.length)
        }

        fun sized(value: String) = apply {
            u32(value.length)
            ascii(value)
        }

        fun zeros(count: Int) = apply { repeat(count) { u8(0) } }

        fun bytes(value: ByteArray) = apply { out.write(value) }

        fun chunk(tag: String, payload: Payload) = apply {
            val content = payload.toByteArray()
            ascii(tag)
            u32(content.size)
            bytes(content)
        }

        fun toByteArray(): ByteArray = out.toByteArray()
    }

    fun write(project: FlmProject, savedAt: LocalDateTime = LocalDateTime.now()): ByteArray {
        val file = Payload()
            .ascii(MAGIC)
            .chunk("HEAD", head(project))
            .chunk("KEYB", keyboard())
            .chunk("META", meta(project, savedAt))
            .chunk("TDIV", Payload().u8(project.timeSigNumerator).u8(project.timeSigDenominator))
            .chunk("RACK", rack(master = true))
        project.tracks.forEach { _ -> file.chunk("RACK", rack(master = false)) }
        file.chunk("CHNL", masterChannel())
        project.tracks.forEachIndexed { index, track -> file.chunk("CHNL", channel(track, index + 1)) }
        return file.toByteArray()
    }

    fun asciiSafe(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD)
        val builder = StringBuilder(decomposed.length)
        for (character in decomposed) {
            when {
                character.code in 0x20..0x7E -> builder.append(character)
                Character.getType(character) == Character.NON_SPACING_MARK.toInt() -> Unit
                else -> builder.append('_')
            }
        }
        return builder.toString().trim()
    }

    private fun safeName(value: String, fallback: String): String =
        asciiSafe(value).ifEmpty { fallback }.take(NAME_MAX_LENGTH)

    private fun head(project: FlmProject): Payload {
        val channelCount = project.tracks.size + 1
        val uuid = UUID.randomUUID()
        return Payload()
            .u32(FILE_VERSION)
            .u32(0)
            .field(safeName(project.title, "Untitled"), TEXT_FIELD_SIZE)
            .f64(project.tempo)
            .zeros(32)
            .bytes(VIEW_TAIL)
            .bytes(ByteBuffer.allocate(16).putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits).array())
            .u32(channelCount + 1)
            .u32(0)
            .u8(1)
            .u8(1)
            .u32(channelCount)
            .sized(APP_VERSION)
            .u8(1)
    }

    private fun keyboard(): Payload {
        val entries = CHORDS.joinToString(",\n") { (base, octave, quality) ->
            "        {\n" +
                "            \"baseNote\": $base,\n" +
                "            \"extensions\": [],\n" +
                "            \"octave\": $octave,\n" +
                "            \"quality\": $quality\n" +
                "        }"
        }
        val chords = "{\n    \"chords\": [\n$entries\n    ],\n    \"rootNote\": 0\n}"
        return Payload()
            .sized(chords)
            .zeros(8)
            .sized("Major")
            .zeros(7)
            .u8(0x3F)
            .zeros(5)
    }

    private fun meta(project: FlmProject, savedAt: LocalDateTime): Payload {
        val created = savedAt.format(CREATED_FORMAT)
        val saved = savedAt.format(SAVED_FORMAT)
        val title = safeName(project.title, "Untitled")
        val payload = Payload()
            .field(created, META_TITLE_OFFSET)
            .field(title, META_SAVED_OFFSET - META_TITLE_OFFSET)
            .ascii(saved)
        return payload.zeros(META_SIZE - META_SAVED_OFFSET - saved.length)
    }

    private fun rack(master: Boolean): Payload {
        val header = Payload()
            .u32(-1)
            .u32(if (master) 1 else SAMPLER_MODULE_ID)
            .u32(0)
            .u32(-1)
        val levels = Payload()
            .u32(ROW_VOLUME_BITS)
            .f32(PAN_CENTER)
            .u32(0)
            .u32(0)
            .f32(PAN_CENTER)
            .u32(0)
        val rack = Payload()
            .u32(if (master) FLAGS_MASTER else FLAGS_CHANNEL)
            .ascii(RACK_MAGIC)
            .chunk("RHED", header)
            .chunk("RPRM", levels)
            .chunk("RSMP", Payload().bytes(PADS_MAP))
        if (!master) rack.chunk("RMOd", samplerModule())
        return rack
    }

    private fun samplerModule(): Payload {
        val header = Payload().u32(SAMPLER_MODULE_TYPE).u32(0).u8(1)
        val description = Payload()
            .field(DEFAULT_PRESET_FOLDER, TEXT_FIELD_SIZE)
            .field(DEFAULT_PRESET_FOLDER, TEXT_FIELD_SIZE)
            .u16(0)
            .u16(0)
        val preset = Payload().u32(0).sized(DEFAULT_PRESET)
        val path = Payload()
            .u32(4)
            .u8(0x40)
            .u8(0x4C)
            .u8(0x40)
            .u8(0)
            .u32(0)
            .f32(0.25f)
        val custom = Payload()
            .ascii(CUSTOM_MAGIC)
            .chunk("PATh", path)
            .chunk("MIDI", Payload().u16(2))
        return Payload()
            .u32(SAMPLER_MODULE_TYPE)
            .u32(SAMPLER_MODULE_ID)
            .chunk("HEAD", header)
            .chunk("DESC", description)
            .chunk("PRST", preset)
            .chunk("ADD1", Payload().u8(1).u8(0).u8(0x7F))
            .chunk("PRMS", Payload().bytes(SAMPLER_PARAMS))
            .chunk("SMPR", Payload().bytes(SAMPLE_RANGE))
            .chunk("CSTM", custom)
    }

    private fun masterChannel(): Payload {
        val header = Payload()
            .field(MASTER_NAME, CHANNEL_NAME_SIZE)
            .bytes(MASTER_TAIL)
            .sized(MASTER_NAME)
            .u32(0)
            .u32(0)
        return Payload()
            .u32(FLAGS_MASTER)
            .ascii(CHANNEL_MAGIC)
            .chunk("CHHD", header)
            .chunk("TRKH", Payload().chunk("DESc", masterTrackDescription()))
    }

    private fun masterTrackDescription(): Payload =
        Payload()
            .u32(1)
            .zeros(24)
            .field("Level", CHANNEL_NAME_SIZE)
            .u32(0)

    private fun channel(track: FlmTrack, id: Int): Payload {
        val name = safeName(track.name, "Channel")
        val header = Payload()
            .field(name, CHANNEL_NAME_SIZE)
            .u32(1)
            .f64(id.toDouble())
            .bytes(CHANNEL_LEVELS)
            .u32(0)
            .bytes(SAMPLER_GUID)
            .u32(id)
            .sized(SAMPLER_NAME)
            .sized("")
            .sized(name)
        val clips = Payload().chunk("DESc", trackDescription(name, if (track.notes.isEmpty()) 0 else 1))
        if (track.notes.isNotEmpty()) clips.chunk("CLIP", clip(track.notes))
        return Payload()
            .u32(FLAGS_CHANNEL)
            .ascii(CHANNEL_MAGIC)
            .chunk("CHHD", header)
            .chunk("SEND", Payload().bytes(SEND_LEVELS))
            .chunk("TRKH", clips)
    }

    private fun trackDescription(name: String, lastClipId: Int): Payload =
        Payload()
            .u32(0)
            .u32(-1)
            .u32(-1)
            .u32(0)
            .u32(0)
            .u32(lastClipId)
            .u32(1)
            .field(name, CHANNEL_NAME_SIZE)
            .u32(0)

    private fun clip(notes: List<FlmNote>): Payload {
        val end = notes.maxOf { it.positionBeats + it.lengthBeats }
        val header = Payload()
            .f64(ceil(end * 4.0) / 4.0)
            .f64(loopLength(end))
            .f64(0.0)
            .u32(1)
            .zeros(9)
        val records = Payload().u16(NOTE_RECORD_SIZE)
        notes.forEach { writeNote(records, it) }
        return Payload()
            .u32(0)
            .ascii(CLIP_MAGIC)
            .chunk("CLHd", header)
            .chunk("ZOOM", Payload().f64(0.0).f64(60.0).f64(0.25).f64(1.0))
            .chunk("EVN2", records)
    }

    private fun loopLength(end: Double): Double {
        var length = MIN_CLIP_BEATS
        while (length < end) length *= 2.0
        return length
    }

    private fun writeNote(records: Payload, note: FlmNote) {
        val position = (note.positionBeats * TICKS_PER_BEAT).roundToLong().coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        val release = (note.release.toDouble() / DEFAULT_RELEASE).coerceIn(0.0, 1.0)
        records
            .u32(position)
            .f64(note.lengthBeats.coerceAtLeast(MIN_LENGTH_BEATS))
            .u16(note.key.coerceIn(0, MAX_KEY))
            .u16(scaleLevel(note.velocity.coerceIn(1, MAX_LEVEL)))
            .u16((release * LEVEL_MAX).roundToInt())
            .u16(0)
    }

    private fun scaleLevel(value: Int): Int = (value.toDouble() / MAX_LEVEL * LEVEL_MAX).roundToInt()
}
