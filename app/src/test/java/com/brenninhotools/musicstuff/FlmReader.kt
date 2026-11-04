package com.brenninhotools.musicstuff

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ReadChunk(val tag: String, val start: Int, val end: Int)

data class ReadNote(val beats: Double, val lengthBeats: Float, val key: Int, val velocity: Int, val slide: Boolean)

data class ReadChannel(val name: String, val notes: List<ReadNote>)

class FlmReader(private val data: ByteArray) {
    private val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    fun chunks(start: Int = 4, end: Int = data.size): List<ReadChunk> {
        val chunks = mutableListOf<ReadChunk>()
        var offset = start
        while (offset < end) {
            check(offset + 8 <= end) { "Truncated chunk header at $offset" }
            val tag = String(data, offset, 4, Charsets.ISO_8859_1)
            check(tag.all { it in 'A'..'Z' || it in '0'..'9' }) { "Bad tag '$tag' at $offset" }
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

    fun channels(): List<ReadChannel> = chunks().filter { it.tag == "CHNL" }.map(::channel)

    private fun channel(chunk: ReadChunk): ReadChannel {
        val name = terminated(chunk.start + 16)
        val tracks = chunks(chunk.start + 16 + name.length + 1, chunk.end).single { it.tag == "TRKH" }
        val notes = chunks(tracks.start, tracks.end).filter { it.tag == "CLIP" }.flatMap { clip ->
            val events = chunks(clip.start + 4, clip.end).single { it.tag == "EVN2" }
            notes(events)
        }
        return ReadChannel(name, notes)
    }

    private fun notes(chunk: ReadChunk): List<ReadNote> {
        check(buffer.getInt(chunk.start) == 20)
        val count = (chunk.end - chunk.start - 4 - 18) / 20
        return (0 until count).map { index ->
            val offset = chunk.start + 4 + index * 20
            val raw = u8(offset + 17) or (u8(offset + 18) shl 8) or (u8(offset + 19) shl 16)
            ReadNote(
                beats = raw / 16384.0,
                lengthBeats = buffer.getFloat(offset + 6),
                key = u8(offset + 10),
                velocity = buffer.getShort(offset + 12).toInt() and 0xFFFF,
                slide = u8(offset + 16) and 0x08 != 0,
            )
        }
    }

    private fun u8(offset: Int): Int = data[offset].toInt() and 0xFF

    private fun terminated(start: Int): String {
        var end = start
        while (data[end].toInt() != 0) end++
        return String(data, start, end - start, Charsets.ISO_8859_1)
    }
}
