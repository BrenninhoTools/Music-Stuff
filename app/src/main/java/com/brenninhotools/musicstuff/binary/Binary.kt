package com.brenninhotools.musicstuff.binary

class ByteSink(initialCapacity: Int = 256) {
    private var data = ByteArray(initialCapacity)

    var size: Int = 0
        private set

    private fun reserve(extra: Int) {
        if (size + extra <= data.size) return
        var capacity = data.size
        while (capacity < size + extra) capacity *= 2
        data = data.copyOf(capacity)
    }

    fun u8(value: Int) {
        reserve(1)
        data[size] = value.toByte()
        size++
    }

    fun u16(value: Int) {
        u8(value)
        u8(value shr 8)
    }

    fun u32(value: Int) {
        u16(value)
        u16(value ushr 16)
    }

    fun f32(value: Float) = u32(value.toRawBits())

    fun f64(value: Double) {
        val bits = value.toRawBits()
        u32(bits.toInt())
        u32((bits ushr 32).toInt())
    }

    fun bytes(value: ByteArray) {
        reserve(value.size)
        value.copyInto(data, size)
        size += value.size
    }

    fun latin1(value: String) = value.forEach { u8(it.code) }

    fun utf16le(value: String) = value.forEach { u16(it.code) }

    fun zeros(count: Int) = repeat(count) { u8(0) }

    fun toByteArray(): ByteArray = data.copyOf(size)
}

fun ByteArray.u8At(offset: Int): Int = this[offset].toInt() and 0xFF

fun ByteArray.u16At(offset: Int): Int = u8At(offset) or (u8At(offset + 1) shl 8)

fun ByteArray.i32At(offset: Int): Int = u16At(offset) or (u16At(offset + 2) shl 16)

fun ByteArray.u32At(offset: Int): Long = i32At(offset).toLong() and 0xFFFFFFFFL

fun ByteArray.f32At(offset: Int): Float = Float.fromBits(i32At(offset))

fun ByteArray.f64At(offset: Int): Double = Double.fromBits(u32At(offset) or (u32At(offset + 4) shl 32))

fun ByteArray.latin1At(offset: Int, size: Int): String =
    CharArray(size) { Char(u8At(offset + it)) }.concatToString()

fun ByteArray.utf16leAt(offset: Int, size: Int): String =
    CharArray(size / 2) { Char(u16At(offset + it * 2)) }.concatToString()

fun hexBytes(value: String): ByteArray =
    ByteArray(value.length / 2) { value.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
