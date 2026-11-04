package com.brenninhotools.musicstuff.flp

data class FlpProject(
    val title: String,
    val version: String,
    val ppq: Int,
    val tempo: Double,
    val timeSigNumerator: Int,
    val timeSigDenominator: Int,
    val channels: List<FlpChannel>,
    val patterns: List<FlpPattern>,
    val arrangements: List<FlpArrangement>,
    val warnings: List<String>,
) {
    val noteCount: Int get() = patterns.sumOf { it.notes.size }

    fun noteCountFor(channel: FlpChannel): Int =
        patterns.sumOf { pattern -> pattern.notes.count { it.channelIid == channel.iid } }
}

enum class FlpChannelKind(val code: Int, val label: String) {
    SAMPLER(0, "Sampler"),
    NATIVE(2, "Native"),
    LAYER(3, "Layer"),
    INSTRUMENT(4, "Instrument"),
    AUTOMATION(5, "Automation"),
    UNKNOWN(-1, "Unknown");

    companion object {
        fun fromCode(code: Int): FlpChannelKind = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

data class FlpChannel(
    val iid: Int,
    val name: String,
    val kind: FlpChannelKind,
)

data class FlpPattern(
    val iid: Int,
    val name: String,
    val notes: List<FlpNote>,
) {
    val lengthTicks: Long get() = notes.maxOfOrNull { it.position + it.length } ?: 0L
}

data class FlpNote(
    val position: Long,
    val length: Long,
    val key: Int,
    val velocity: Int,
    val release: Int,
    val channelIid: Int,
    val slide: Boolean,
)

data class FlpArrangement(
    val id: Int,
    val name: String,
    val clips: List<FlpClip>,
) {
    val patternClips: List<FlpClip> get() = clips.filter { it.patternIid != null }
}

data class FlpClip(
    val position: Long,
    val length: Long,
    val patternIid: Int?,
    val startOffset: Float,
    val track: Int = 0,
)
