package com.brenninhotools.musicstuff.flm

data class FlmProject(
    val title: String,
    val tempo: Double,
    val timeSigNumerator: Int,
    val timeSigDenominator: Int,
    val tracks: List<FlmTrack>,
)

data class FlmTrack(
    val name: String,
    val notes: List<FlmNote>,
)

data class FlmNote(
    val positionBeats: Double,
    val lengthBeats: Double,
    val key: Int,
    val velocity: Int,
    val release: Int,
    val slide: Boolean,
)
