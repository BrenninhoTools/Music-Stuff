package com.brenninhotools.musicstuff

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ReadChunk(val tag: String, val start: Int, val end: Int)

data class ReadNote(val position: Int, val lengthBeats: Double, val key: Int, val velocity: Int, val release: Int)

data class ReadClip(val position: Int, val contentBeats: Double, val loopBeats: Double, val id: Int, val notes: List<ReadNote>)

data class ReadChannel(val name: String, val clips: List<ReadClip>) {
    val notes: List<ReadNote> get() = clips.flatMap { it.notes }
}

class FlmReader(private val data: ByteArray) {
    private val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    fun chunks(start: Int = 4, end: Int = data.size): List<ReadChunk> {
        val chunks = mutableListOf<ReadChunk>()
        var offset = start
        while (offset < end) {
            check(offset + 8 <= end) { "Truncated chunk header at $offset" }
            val tag = String(data, offset, 4, Charsets.ISO_8859_1)
            check(tag.all { it.isLetterOrDigit() }) { "Bad tag '$tag' at $offset" }
            val size = buffer.getInt(offset + 4)
            val payload = offset + 8
            check(size >= 0 && payload + size <= end) { "Chunk $tag overruns its parent" }
            chunks += ReadChunk(tag, payload, payload + size)
            offset = payload + size
        }
        return chunks
    }

    fun title(): String = terminated(chunks().first { it.tag == "HEAD" }.start + 8)

    fun tempo(): Double = buffer.getDouble(chunks().first { it.tag == "HEAD" }.start + 264)

    fun timeSignature(): Pair<Int, Int> {
        val chunk = chunks().first { it.tag == "TDIV" }
        return data[chunk.start].toInt() to data[chunk.start + 1].toInt()
    }

    fun racks(): List<List<String>> = chunks().filter { it.tag == "RACK" }.map { rack ->
        check(String(data, rack.start + 4, 4, Charsets.ISO_8859_1) == "10KR")
        chunks(rack.start + 8, rack.end).map { it.tag }
    }

    fun channelHeaderSizes(): List<Int> = chunks().filter { it.tag == "CHNL" }.map { channel ->
        chunks(channel.start + 8, channel.end).first { it.tag == "CHHD" }.let { it.end - it.start }
    }

    fun channels(): List<ReadChannel> = chunks().filter { it.tag == "CHNL" }.map(::channel)

    private fun channel(chunk: ReadChunk): ReadChannel {
        check(String(data, chunk.start + 4, 4, Charsets.ISO_8859_1) == "20HC")
        val parts = chunks(chunk.start + 8, chunk.end)
        val header = parts.first { it.tag == "CHHD" }
        val tracks = parts.filter { it.tag == "TRKH" }
        val clips = tracks.flatMap { track -> chunks(track.start, track.end).filter { it.tag == "CLIP" } }.map(::clip)
        return ReadChannel(terminated(header.start), clips)
    }

    private fun clip(chunk: ReadChunk): ReadClip {
        check(String(data, chunk.start + 4, 4, Charsets.ISO_8859_1) == "20LC")
        val parts = chunks(chunk.start + 8, chunk.end)
        val header = parts.first { it.tag == "CLHd" }
        check(header.end - header.start == 37)
        check(parts.first { it.tag == "ZOOM" }.let { it.end - it.start } == 32)
        val events = parts.first { it.tag == "EVN2" }
        return ReadClip(
            position = buffer.getInt(chunk.start),
            contentBeats = buffer.getDouble(header.start),
            loopBeats = buffer.getDouble(header.start + 8),
            id = buffer.getInt(header.start + 24),
            notes = notes(events),
        )
    }

    private fun notes(chunk: ReadChunk): List<ReadNote> {
        check(buffer.getShort(chunk.start).toInt() == 20)
        check((chunk.end - chunk.start - 2) % 20 == 0)
        return (0 until (chunk.end - chunk.start - 2) / 20).map { index ->
            val offset = chunk.start + 2 + index * 20
            ReadNote(
                position = buffer.getInt(offset),
                lengthBeats = buffer.getDouble(offset + 4),
                key = buffer.getShort(offset + 12).toInt() and 0xFFFF,
                velocity = buffer.getShort(offset + 14).toInt() and 0xFFFF,
                release = buffer.getShort(offset + 16).toInt() and 0xFFFF,
            )
        }
    }

    private fun terminated(start: Int): String {
        var end = start
        while (data[end].toInt() != 0) end++
        return String(data, start, end - start, Charsets.ISO_8859_1)
    }
}
