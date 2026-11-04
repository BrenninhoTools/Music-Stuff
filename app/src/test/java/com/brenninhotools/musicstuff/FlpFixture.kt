package com.brenninhotools.musicstuff

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class FlpFixture(private val ppq: Int = 96) {
    private val events = ByteArrayOutputStream()

    fun byte(id: Int, value: Int) = apply {
        events.write(id)
        events.write(value)
    }

    fun word(id: Int, value: Int) = apply {
        events.write(id)
        events.write(value and 0xFF)
        events.write(value shr 8)
    }

    fun dword(id: Int, value: Int) = apply {
        events.write(id)
        events.write(le(4).putInt(value).array())
    }

    fun data(id: Int, value: ByteArray) = apply {
        events.write(id)
        var size = value.size
        while (size >= 0x80) {
            events.write((size and 0x7F) or 0x80)
            size = size shr 7
        }
        events.write(size)
        events.write(value)
    }

    fun raw(id: Int, vararg bytes: Int) = apply {
        events.write(id)
        bytes.forEach(events::write)
    }

    fun text(id: Int, value: String) = data(id, (value + "\u0000").toByteArray(Charsets.UTF_16LE))

    fun version(value: String) = data(199, (value + "\u0000").toByteArray(Charsets.US_ASCII))

    fun notes(vararg notes: ByteArray) = data(224, notes.reduce(ByteArray::plus))

    fun build(): ByteArray {
        val body = events.toByteArray()
        val header = le(22)
            .put("FLhd".toByteArray())
            .putInt(6)
            .putShort(0)
            .putShort(1)
            .putShort(ppq.toShort())
            .put("FLdt".toByteArray())
            .putInt(body.size)
            .array()
        return header + body
    }

    companion object {
        private fun le(size: Int): ByteBuffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

        fun note(
            position: Int,
            length: Int,
            key: Int,
            channel: Int = 0,
            velocity: Int = 100,
            release: Int = 64,
            slide: Boolean = false,
        ): ByteArray {
            val buffer = le(24)
                .putInt(position)
                .putShort((if (slide) 0x0008 else 0).toShort())
                .putShort(channel.toShort())
                .putInt(length)
                .putShort(key.toShort())
                .putShort(0)
            buffer.put(16, 120.toByte())
            buffer.put(18, release.toByte())
            buffer.put(20, 64.toByte())
            buffer.put(21, velocity.toByte())
            return buffer.array()
        }

        fun clip(
            position: Int,
            patternIid: Int,
            length: Int,
            startOffset: Int = -1,
            itemSize: Int = 32,
        ): ByteArray = le(itemSize)
            .putInt(position)
            .putShort(20480.toShort())
            .putShort((20480 + patternIid).toShort())
            .putInt(length)
            .putShort(499)
            .putShort(0)
            .putInt(0)
            .putInt(0)
            .putInt(startOffset)
            .putInt(-1)
            .array()

        fun channelClip(position: Int, channelIid: Int, length: Int): ByteArray = le(32)
            .putInt(position)
            .putShort(20480.toShort())
            .putShort(channelIid.toShort())
            .putInt(length)
            .array()
    }
}
