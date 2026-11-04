package com.brenninhotools.musicstuff.flm

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class FlmParser private constructor(private val data: ByteArray) {
    private class Chunk(val tag: String, val start: Int, val end: Int)

    private val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    private var audioClips = 0
    private var automationTracks = 0
    private var unreadBlocks = 0

    private fun parse(): FlmDocument {
        if (data.size < MIN_SIZE) throw FlmFormatException("The file is too small to be an .flm project.")
        if (ascii(0, 4) != MAGIC) throw FlmFormatException("This is not an FL Studio Mobile project (missing 10LF header).")
        val chunks = chunks(4, data.size)
        val head = chunks.firstOrNull { it.tag == "HEAD" }
            ?: throw FlmFormatException("The project header (HEAD) is missing.")
        if (head.end - head.start < HEAD_MIN_SIZE) throw FlmFormatException("The project header (HEAD) is incomplete.")

        val divisions = chunks.firstOrNull { it.tag == "TDIV" }
        val numerator = divisions?.let { byteAt(it.start) }?.takeIf { it > 0 } ?: DEFAULT_TIME_SIG
        val denominator = divisions?.let { byteAt(it.start + 1) }?.takeIf { it > 0 } ?: DEFAULT_TIME_SIG
        val channels = chunks.filter { it.tag == "CHNL" }.mapNotNull(::channel)

        val warnings = mutableListOf<String>()
        if (audioClips > 0) warnings += "$audioClips audio clip(s) were skipped. Only notes are converted."
        if (automationTracks > 0) warnings += "$automationTracks automation track(s) were skipped."
        if (unreadBlocks > 0) warnings += "$unreadBlocks block(s) had an unexpected layout and were skipped."
        return FlmDocument(
            title = text(head.start + TITLE_OFFSET, TITLE_SIZE).ifEmpty { "Untitled" },
            tempo = buffer.getDouble(head.start + TEMPO_OFFSET).takeIf { it > 0.0 } ?: DEFAULT_TEMPO,
            timeSigNumerator = numerator,
            timeSigDenominator = denominator,
            appVersion = appVersion(head),
            channels = channels,
            warnings = warnings,
        )
    }

    private fun appVersion(head: Chunk): String {
        val offset = head.start + VERSION_OFFSET
        if (offset + 4 > head.end) return ""
        val size = buffer.getInt(offset)
        if (size !in 1..MAX_VERSION_SIZE || offset + 4 + size > head.end) return ""
        return ascii(offset + 4, size)
    }

    private fun channel(chunk: Chunk): FlmChannel? {
        if (chunk.end - chunk.start < 8 || ascii(chunk.start + 4, 4) != CHANNEL_MAGIC) {
            unreadBlocks++
            return null
        }
        val parts = chunks(chunk.start + 8, chunk.end)
        val header = parts.firstOrNull { it.tag == "CHHD" }
        if (header == null) {
            unreadBlocks++
            return null
        }
        val clips = parts.filter { it.tag == "TRKH" }.flatMap(::track)
        return FlmChannel(text(header.start, minOf(CHANNEL_NAME_SIZE, header.end - header.start)), clips)
    }

    private fun track(chunk: Chunk): List<FlmClip> {
        val parts = chunks(chunk.start, chunk.end)
        val description = parts.firstOrNull { it.tag == "DESc" } ?: return emptyList()
        val clips = parts.filter { it.tag == "CLIP" }
        return when (buffer.getInt(description.start)) {
            TRACK_NOTES -> clips.mapNotNull(::clip)
            TRACK_AUDIO -> {
                audioClips += clips.size
                emptyList()
            }
            else -> {
                if (clips.isNotEmpty()) automationTracks++
                emptyList()
            }
        }
    }

    private fun clip(chunk: Chunk): FlmClip? {
        if (chunk.end - chunk.start < 8 || ascii(chunk.start + 4, 4) != CLIP_MAGIC) {
            unreadBlocks++
            return null
        }
        val parts = chunks(chunk.start + 8, chunk.end)
        val header = parts.firstOrNull { it.tag == "CLHd" }
        val events = parts.firstOrNull { it.tag == "EVN2" }
        if (header == null || events == null || header.end - header.start < 8) {
            unreadBlocks++
            return null
        }
        val notes = notes(events)
        if (notes.isEmpty()) return null
        val end = notes.maxOf { it.positionBeats + it.lengthBeats }
        val length = buffer.getDouble(header.start).takeIf { it > 0.0 } ?: end
        return FlmClip(
            positionBeats = (buffer.getInt(chunk.start).toLong() and 0xFFFFFFFFL) / FlmWriter.TICKS_PER_BEAT.toDouble(),
            lengthBeats = maxOf(length, end),
            notes = notes,
        )
    }

    private fun notes(chunk: Chunk): List<FlmNote> {
        val size = chunk.end - chunk.start
        if (size < 2 || (size - 2) % RECORD_SIZE != 0 || buffer.getShort(chunk.start).toInt() != RECORD_SIZE) {
            unreadBlocks++
            return emptyList()
        }
        return (0 until (size - 2) / RECORD_SIZE).map { index ->
            val offset = chunk.start + 2 + index * RECORD_SIZE
            FlmNote(
                positionBeats = (buffer.getInt(offset).toLong() and 0xFFFFFFFFL) / FlmWriter.TICKS_PER_BEAT.toDouble(),
                lengthBeats = buffer.getDouble(offset + 4),
                key = buffer.getShort(offset + 12).toInt() and 0xFFFF,
                velocity = scale(offset + 14, FlmWriter.MAX_LEVEL).coerceAtLeast(1),
                release = scale(offset + 16, DEFAULT_RELEASE),
            )
        }
    }

    private fun scale(offset: Int, maximum: Int): Int {
        val value = buffer.getShort(offset).toInt() and 0xFFFF
        return (value.toDouble() / LEVEL_MAX * maximum).roundToInt().coerceIn(0, maximum)
    }

    private fun chunks(start: Int, end: Int): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        var offset = start
        while (offset + CHUNK_HEADER <= end) {
            val tag = ascii(offset, 4)
            val size = buffer.getInt(offset + 4).toLong() and 0xFFFFFFFFL
            val payload = offset + CHUNK_HEADER
            if (!tag.all { it.isLetterOrDigit() } || payload + size > end) {
                unreadBlocks++
                break
            }
            chunks += Chunk(tag, payload, payload + size.toInt())
            offset = payload + size.toInt()
        }
        return chunks
    }

    private fun byteAt(offset: Int): Int = data[offset].toInt() and 0xFF

    private fun ascii(offset: Int, size: Int): String = String(data, offset, size, Charsets.ISO_8859_1)

    private fun text(offset: Int, size: Int): String {
        var end = offset
        while (end < offset + size && data[end].toInt() != 0) end++
        return String(data, offset, end - offset, Charsets.UTF_8).trim()
    }

    companion object {
        private const val MAGIC = "10LF"
        private const val CHANNEL_MAGIC = "20HC"
        private const val CLIP_MAGIC = "20LC"
        private const val MIN_SIZE = 12
        private const val CHUNK_HEADER = 8
        private const val HEAD_MIN_SIZE = 272
        private const val TITLE_OFFSET = 8
        private const val TITLE_SIZE = 256
        private const val TEMPO_OFFSET = 264
        private const val VERSION_OFFSET = 358
        private const val MAX_VERSION_SIZE = 16
        private const val CHANNEL_NAME_SIZE = 1024
        private const val RECORD_SIZE = 20
        private const val TRACK_NOTES = 0
        private const val TRACK_AUDIO = 2
        private const val LEVEL_MAX = 32767
        private const val DEFAULT_RELEASE = 64
        private const val DEFAULT_TIME_SIG = 4
        private const val DEFAULT_TEMPO = 140.0

        @Throws(FlmFormatException::class)
        fun parse(data: ByteArray): FlmDocument = FlmParser(data).parse()
    }
}
