package com.brenninhotools.musicstuff.ui

import com.brenninhotools.musicstuff.flm.FlmProject
import com.brenninhotools.musicstuff.flm.FlmTrack

const val ACTIVITY_SLICES = 16

fun FlmProject.endBeats(): Double =
    tracks.maxOfOrNull { track -> track.notes.maxOfOrNull { it.positionBeats + it.lengthBeats } ?: 0.0 } ?: 0.0

fun FlmTrack.activity(endBeats: Double): List<Boolean> {
    val slices = BooleanArray(ACTIVITY_SLICES)
    if (endBeats <= 0.0) return slices.toList()
    notes.forEach { note ->
        val slice = (note.positionBeats / endBeats * ACTIVITY_SLICES).toInt().coerceIn(0, ACTIVITY_SLICES - 1)
        slices[slice] = true
    }
    return slices.toList()
}
