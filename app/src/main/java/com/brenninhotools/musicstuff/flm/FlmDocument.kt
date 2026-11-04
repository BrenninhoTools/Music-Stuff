package com.brenninhotools.musicstuff.flm

class FlmFormatException(message: String) : Exception(message)

data class FlmDocument(
    val title: String,
    val tempo: Double,
    val timeSigNumerator: Int,
    val timeSigDenominator: Int,
    val appVersion: String,
    val channels: List<FlmChannel>,
    val warnings: List<String>,
) {
    val noteCount: Int get() = channels.sumOf { channel -> channel.clips.sumOf { it.notes.size } }

    fun flattened(): FlmProject = FlmProject(
        title = title,
        tempo = tempo,
        timeSigNumerator = timeSigNumerator,
        timeSigDenominator = timeSigDenominator,
        tracks = channels.filter { it.clips.isNotEmpty() }.map { channel ->
            FlmTrack(
                name = channel.name,
                notes = channel.clips.flatMap { clip ->
                    clip.notes.map { it.copy(positionBeats = clip.positionBeats + it.positionBeats) }
                }.sortedBy { it.positionBeats },
            )
        },
    )
}

data class FlmChannel(
    val name: String,
    val clips: List<FlmClip>,
)

data class FlmClip(
    val positionBeats: Double,
    val lengthBeats: Double,
    val notes: List<FlmNote>,
)
