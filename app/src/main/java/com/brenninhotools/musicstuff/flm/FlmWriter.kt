package com.brenninhotools.musicstuff.flm

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object FlmWriter {
    const val POSITION_SCALE = 16384
    const val MAX_POSITION_RAW = 0xFFFFFF
    const val MAX_POSITION_BEATS = MAX_POSITION_RAW.toDouble() / POSITION_SCALE
    const val MAX_KEY = 127
    const val MAX_LEVEL = 127
    const val MASTER_NAME = "MASTER"

    private const val MAGIC = "10LF"
    private const val APP_VERSION = "4.10.15"
    private const val CHANNEL_MAGIC = "20HCCHHD"
    private const val NOTE_RECORD_SIZE = 20
    private const val NOTE_LIST_TRAILER = 18
    private const val TITLE_FIELD_SIZE = 256
    private const val NAME_MAX_LENGTH = 127
    private const val LEVEL_SCALE = 32767
    private const val SLIDE_FLAG = 0x08
    private const val CHANNEL_FLAGS_INSTRUMENT = 0x00010000
    private const val MIN_LENGTH_BEATS = 0.001f

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

    fun write(project: FlmProject): ByteArray {
        val file = Payload()
            .ascii(MAGIC)
            .chunk("HEAD", head(project))
            .chunk("TDIV", Payload().u8(project.timeSigNumerator).u8(project.timeSigDenominator))
            .chunk("KEYB", keyboard())
            .chunk("CHNL", channel(MASTER_NAME, master = true, notes = emptyList()))
        project.tracks.forEach { track ->
            file.chunk("CHNL", channel(track.name, master = false, notes = track.notes))
        }
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

    private fun head(project: FlmProject): Payload {
        val title = asciiSafe(project.title).ifEmpty { "Untitled" }.take(TITLE_FIELD_SIZE - 1)
        return Payload()
            .u32(2)
            .u32(0)
            .ascii(title)
            .zeros(TITLE_FIELD_SIZE - title.length)
            .f64(project.tempo)
            .zeros(72)
            .u32(0)
            .u32(0)
            .u8(1)
            .u8(1)
            .u8(0)
            .u8(0)
            .u32(APP_VERSION.length)
            .ascii(APP_VERSION)
            .zeros(2)
    }

    private fun keyboard(): Payload {
        val chords = "{\n    \"chords\": [],\n    \"rootNote\": 0\n}"
        val scale = "Major"
        return Payload()
            .u32(chords.length)
            .ascii(chords)
            .u32(scale.length)
            .ascii(scale)
            .zeros(8)
    }

    private fun channel(name: String, master: Boolean, notes: List<FlmNote>): Payload {
        val safeName = asciiSafe(name).ifEmpty { "Channel" }.take(NAME_MAX_LENGTH)
        val clips = Payload()
        if (notes.isNotEmpty()) clips.chunk("CLIP", clip(notes))
        return Payload()
            .u32(if (master) 0 else CHANNEL_FLAGS_INSTRUMENT)
            .ascii(CHANNEL_MAGIC)
            .u8(0x64)
            .u8(0x04)
            .u8(0x00)
            .u8(0xF0)
            .ascii(safeName)
            .u8(0)
            .chunk("TRKH", clips)
    }

    private fun clip(notes: List<FlmNote>): Payload {
        val end = notes.maxOf { it.positionBeats + it.lengthBeats }
        val duration = (end * POSITION_SCALE).roundToLong().coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        val records = Payload().u32(NOTE_RECORD_SIZE)
        notes.forEach { writeNote(records, it) }
        records.zeros(NOTE_LIST_TRAILER)
        return Payload().u32(duration).chunk("EVN2", records)
    }

    private fun writeNote(records: Payload, note: FlmNote) {
        val position = (note.positionBeats * POSITION_SCALE).roundToLong().coerceIn(0L, MAX_POSITION_RAW.toLong()).toInt()
        records
            .zeros(6)
            .f32(max(MIN_LENGTH_BEATS, note.lengthBeats.toFloat()))
            .u8(note.key.coerceIn(0, MAX_KEY))
            .u8(0)
            .u16(scaleLevel(note.velocity.coerceIn(1, MAX_LEVEL)))
            .u16(scaleLevel(note.release.coerceIn(0, MAX_LEVEL)))
            .u8(if (note.slide) SLIDE_FLAG else 0)
            .u8(position)
            .u8(position shr 8)
            .u8(position shr 16)
    }

    private fun scaleLevel(value: Int): Int = (value.toDouble() / MAX_LEVEL * LEVEL_SCALE).roundToInt()
}
