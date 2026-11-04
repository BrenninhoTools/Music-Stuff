package com.brenninhotools.musicstuff.flp

import java.nio.ByteBuffer
import java.nio.ByteOrder

class FlpFormatException(message: String) : Exception(message)

class FlpParser private constructor(private val data: ByteArray) {

    private class ChannelDraft(val iid: Int) {
        var name: String? = null
        var internalName: String? = null
        var kind: FlpChannelKind = FlpChannelKind.SAMPLER
    }

    private class PatternDraft(val iid: Int) {
        var name: String? = null
        val notes = mutableListOf<FlpNote>()
    }

    private class ArrangementDraft(val id: Int) {
        var name: String? = null
        val clips = mutableListOf<FlpClip>()
    }

    private val buffer: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    private val warnings = linkedSetOf<String>()

    private var title = ""
    private var version = ""
    private var versionMajor = 0
    private var versionMinor = 0
    private var ppq = DEFAULT_PPQ
    private var tempoExact: Double? = null
    private var tempoCoarse: Int? = null
    private var tempoFine = 0
    private var timeSigNumerator = 4
    private var timeSigDenominator = 4

    private val channels = linkedMapOf<Int, ChannelDraft>()
    private val patterns = linkedMapOf<Int, PatternDraft>()
    private val arrangements = linkedMapOf<Int, ArrangementDraft>()

    private var currentChannel: ChannelDraft? = null
    private var currentPattern: PatternDraft? = null
    private var currentArrangement: ArrangementDraft? = null
    private var inMixer = false
    private var event172Size = DWORD_SIZE

    private fun parse(): FlpProject {
        val eventsEnd = readHeader()
        event172Size = detectEvent172Size(buffer.position(), eventsEnd)
        while (buffer.position() < eventsEnd) {
            if (!readEvent(eventsEnd)) break
        }
        return buildProject()
    }

    private fun readHeader(): Int {
        if (data.size >= 2 && data[0] == 'P'.code.toByte() && data[1] == 'K'.code.toByte()) {
            throw FlpFormatException("This is a .zip archive. Extract the .flp from it first.")
        }
        if (data.size < HEADER_SIZE) throw FlpFormatException("The file is too small to be an .flp project.")
        if (readMagic() != "FLhd") throw FlpFormatException("This is not an FL Studio project (missing FLhd header).")
        buffer.getInt()
        buffer.getShort()
        buffer.getShort()
        ppq = (buffer.getShort().toInt() and 0xFFFF).takeIf { it > 0 } ?: DEFAULT_PPQ
        if (readMagic() != "FLdt") throw FlpFormatException("The project data chunk (FLdt) is missing.")
        val declared = buffer.getInt().toLong() and 0xFFFFFFFFL
        val available = (data.size - buffer.position()).toLong()
        if (declared > available) warnings += "The file is shorter than its header says, so it may be truncated."
        return (buffer.position() + minOf(declared, available)).toInt()
    }

    private fun readMagic(): String {
        val bytes = ByteArray(4)
        buffer.get(bytes)
        return String(bytes, Charsets.ISO_8859_1)
    }

    private fun readEvent(end: Int): Boolean {
        val id = buffer.get().toInt() and 0xFF
        val remaining = end - buffer.position()
        val fixedSize = when {
            id < FlpEvents.WORD_START -> 1
            id < FlpEvents.DWORD_START -> 2
            id == FlpEvents.SHORT_DWORD -> event172Size
            id < FlpEvents.TEXT_START -> DWORD_SIZE
            else -> 0
        }
        if (fixedSize > remaining) return truncated()
        when {
            fixedSize == 1 -> onByte(id, buffer.get().toInt() and 0xFF)
            fixedSize == 2 -> onWord(id, buffer.getShort().toInt() and 0xFFFF)
            id == FlpEvents.SHORT_DWORD -> buffer.position(buffer.position() + fixedSize)
            fixedSize == DWORD_SIZE -> onDword(id, buffer.getInt().toLong() and 0xFFFFFFFFL)
            else -> {
                val size = readVarInt(end) ?: return truncated()
                if (size > end - buffer.position()) return truncated()
                val start = buffer.position()
                onData(id, start, size.toInt())
                buffer.position(start + size.toInt())
            }
        }
        return true
    }

    private fun detectEvent172Size(start: Int, end: Int): Int =
        if (!framesExactly(start, end, DWORD_SIZE) && framesExactly(start, end, SHORT_EVENT_172_SIZE)) {
            SHORT_EVENT_172_SIZE
        } else {
            DWORD_SIZE
        }

    private fun framesExactly(start: Int, end: Int, size172: Int): Boolean {
        var offset = start
        while (offset < end) {
            val id = data[offset].toInt() and 0xFF
            offset++
            when {
                id < FlpEvents.WORD_START -> offset += 1
                id < FlpEvents.DWORD_START -> offset += 2
                id == FlpEvents.SHORT_DWORD -> offset += size172
                id < FlpEvents.TEXT_START -> offset += DWORD_SIZE
                else -> {
                    var size = 0L
                    var shift = 0
                    while (true) {
                        if (offset >= end || shift >= 35) return false
                        val byte = data[offset].toInt() and 0xFF
                        offset++
                        size = size or ((byte and 0x7F).toLong() shl shift)
                        shift += 7
                        if (byte and 0x80 == 0) break
                    }
                    if (size > end - offset) return false
                    offset += size.toInt()
                }
            }
        }
        return offset == end
    }

    private fun truncated(): Boolean {
        warnings += "The project ended in the middle of an event, so anything after that point was not read."
        return false
    }

    private fun readVarInt(end: Int): Long? {
        var value = 0L
        var shift = 0
        while (buffer.position() < end && shift < 35) {
            val byte = buffer.get().toInt() and 0xFF
            value = value or ((byte and 0x7F).toLong() shl shift)
            if (byte and 0x80 == 0) return value
            shift += 7
        }
        return null
    }

    private fun onByte(id: Int, value: Int) {
        when (id) {
            FlpEvents.CHANNEL_TYPE -> currentChannel?.kind = FlpChannelKind.fromCode(value)
            FlpEvents.TIME_SIG_NUMERATOR -> if (value > 0) timeSigNumerator = value
            FlpEvents.TIME_SIG_DENOMINATOR -> if (value > 0) timeSigDenominator = value
        }
    }

    private fun onWord(id: Int, value: Int) {
        when (id) {
            FlpEvents.CHANNEL_NEW -> {
                val channel = channels.getOrPut(value) { ChannelDraft(value) }
                currentChannel = channel
            }
            FlpEvents.PATTERN_NEW -> currentPattern = patterns.getOrPut(value) { PatternDraft(value) }
            FlpEvents.ARRANGEMENT_NEW -> currentArrangement = arrangements.getOrPut(value) { ArrangementDraft(value) }
            FlpEvents.TEMPO_COARSE -> if (value > 0) tempoCoarse = value
            FlpEvents.TEMPO_FINE -> tempoFine = value
        }
    }

    private fun onDword(id: Int, value: Long) {
        if (id == FlpEvents.TEMPO && value > 0) tempoExact = value / 1000.0
    }

    private fun onData(id: Int, start: Int, size: Int) {
        when (id) {
            FlpEvents.FL_VERSION -> readVersion(start, size)
            FlpEvents.TITLE -> title = decodeText(start, size)
            FlpEvents.PATTERN_NAME -> currentPattern?.name = decodeText(start, size).ifEmpty { null }
            FlpEvents.ARRANGEMENT_NAME -> currentArrangement?.name = decodeText(start, size).ifEmpty { null }
            FlpEvents.CHANNEL_NAME_LEGACY -> {
                val channel = currentChannel
                if (!inMixer && channel != null && channel.name == null) channel.name = decodeText(start, size).ifEmpty { null }
            }
            FlpEvents.PLUGIN_NAME -> if (!inMixer) currentChannel?.name = decodeText(start, size).ifEmpty { null }
            FlpEvents.PLUGIN_INTERNAL_NAME -> if (!inMixer) currentChannel?.internalName = decodeText(start, size).ifEmpty { null }
            FlpEvents.PATTERN_NOTES -> readNotes(start, size)
            FlpEvents.ARRANGEMENT_PLAYLIST -> readPlaylist(start, size)
            FlpEvents.MIXER_PARAMS, FlpEvents.INSERT_FLAGS -> inMixer = true
        }
    }

    private fun readVersion(start: Int, size: Int) {
        version = String(data, start, size, Charsets.US_ASCII).trimEnd('\u0000').trim()
        val parts = version.split('.')
        versionMajor = parts.getOrNull(0)?.toIntOrNull() ?: 0
        versionMinor = parts.getOrNull(1)?.toIntOrNull() ?: 0
    }

    private fun usesUtf16(start: Int, size: Int): Boolean {
        if (version.isNotEmpty()) return versionMajor > 11 || (versionMajor == 11 && versionMinor >= 5)
        if (size < 2 || size % 2 != 0) return false
        return (1 until size step 2).all { data[start + it].toInt() == 0 }
    }

    private fun decodeText(start: Int, size: Int): String {
        val charset = if (usesUtf16(start, size)) Charsets.UTF_16LE else Charsets.ISO_8859_1
        return String(data, start, size, charset).trimEnd('\u0000').trim()
    }

    private fun readNotes(start: Int, size: Int) {
        val pattern = currentPattern ?: return
        if (size % NOTE_SIZE != 0) {
            warnings += "A pattern's note block has an unexpected size, so its notes were skipped."
            return
        }
        for (index in 0 until size / NOTE_SIZE) {
            val offset = start + index * NOTE_SIZE
            val position = buffer.getInt(offset).toLong() and 0xFFFFFFFFL
            val flags = buffer.getShort(offset + 4).toInt() and 0xFFFF
            val channel = buffer.getShort(offset + 6).toInt() and 0xFFFF
            val length = buffer.getInt(offset + 8).toLong() and 0xFFFFFFFFL
            val key = buffer.getShort(offset + 12).toInt() and 0xFFFF
            val release = data[offset + 18].toInt() and 0xFF
            val velocity = data[offset + 21].toInt() and 0xFF
            if (key > MAX_FLP_KEY) continue
            pattern.notes += FlpNote(
                position = position,
                length = length,
                key = key,
                velocity = velocity,
                release = release,
                channelIid = channel,
                slide = flags and NOTE_FLAG_SLIDE != 0,
            )
        }
    }

    private fun playlistItemSize(start: Int, size: Int): Int? =
        PLAYLIST_ITEM_SIZES.firstOrNull { candidate ->
            size % candidate == 0 && (0 until size / candidate).all { index ->
                val base = buffer.getShort(start + index * candidate + 4).toInt() and 0xFFFF
                base == PLAYLIST_PATTERN_BASE
            }
        }

    private fun readPlaylist(start: Int, size: Int) {
        val arrangement = currentArrangement ?: arrangements.getOrPut(0) { ArrangementDraft(0) }.also { currentArrangement = it }
        val itemSize = playlistItemSize(start, size)
        if (itemSize == null) {
            warnings += "A playlist has an unexpected size, so its clips were skipped."
            return
        }
        for (index in 0 until size / itemSize) {
            val offset = start + index * itemSize
            val position = buffer.getInt(offset).toLong() and 0xFFFFFFFFL
            val patternBase = buffer.getShort(offset + 4).toInt() and 0xFFFF
            val itemIndex = buffer.getShort(offset + 6).toInt() and 0xFFFF
            val length = buffer.getInt(offset + 8).toLong() and 0xFFFFFFFFL
            val isPattern = itemIndex > patternBase
            val startOffset = if (isPattern) buffer.getInt(offset + 24).toFloat() else buffer.getFloat(offset + 24)
            arrangement.clips += FlpClip(
                position = position,
                length = length,
                patternIid = (itemIndex - patternBase).takeIf { isPattern },
                startOffset = startOffset,
            )
        }
    }

    private fun resolveTempo(): Double {
        tempoExact?.let { return it }
        tempoCoarse?.let { return it + tempoFine / 1000.0 }
        warnings += "No tempo was found in the project, so $DEFAULT_TEMPO BPM was used."
        return DEFAULT_TEMPO
    }

    private fun buildProject(): FlpProject {
        val builtChannels = channels.values.mapIndexed { index, draft ->
            FlpChannel(
                iid = draft.iid,
                name = draft.name ?: draft.internalName ?: "Channel ${index + 1}",
                kind = draft.kind,
            )
        }
        val builtPatterns = patterns.values.map { draft ->
            FlpPattern(
                iid = draft.iid,
                name = draft.name ?: "Pattern ${draft.iid}",
                notes = draft.notes.toList(),
            )
        }
        val builtArrangements = arrangements.values.map { draft ->
            FlpArrangement(
                id = draft.id,
                name = draft.name ?: "Arrangement ${draft.id + 1}",
                clips = draft.clips.toList(),
            )
        }
        val tempo = resolveTempo()
        if (builtChannels.isEmpty()) warnings += "No channels were found in this project."
        return FlpProject(
            title = title.ifEmpty { "Untitled" },
            version = version,
            ppq = ppq,
            tempo = tempo,
            timeSigNumerator = timeSigNumerator,
            timeSigDenominator = timeSigDenominator,
            channels = builtChannels,
            patterns = builtPatterns,
            arrangements = builtArrangements,
            warnings = warnings.toList(),
        )
    }

    companion object {
        private const val HEADER_SIZE = 22
        private const val NOTE_SIZE = 24
        private const val DWORD_SIZE = 4
        private const val SHORT_EVENT_172_SIZE = 3
        private const val PLAYLIST_PATTERN_BASE = 20480
        private val PLAYLIST_ITEM_SIZES = listOf(88, 60, 32)
        private const val NOTE_FLAG_SLIDE = 0x0008
        private const val MAX_FLP_KEY = 131
        private const val DEFAULT_PPQ = 96
        private const val DEFAULT_TEMPO = 140.0

        @Throws(FlpFormatException::class)
        fun parse(data: ByteArray): FlpProject = FlpParser(data).parse()
    }
}
