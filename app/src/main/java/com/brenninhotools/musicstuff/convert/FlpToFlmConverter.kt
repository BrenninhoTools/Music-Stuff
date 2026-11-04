package com.brenninhotools.musicstuff.convert

import com.brenninhotools.musicstuff.flm.FlmNote
import com.brenninhotools.musicstuff.flm.FlmProject
import com.brenninhotools.musicstuff.flm.FlmTrack
import com.brenninhotools.musicstuff.flm.FlmWriter
import com.brenninhotools.musicstuff.flp.FlpArrangement
import com.brenninhotools.musicstuff.flp.FlpChannelKind
import com.brenninhotools.musicstuff.flp.FlpNote
import com.brenninhotools.musicstuff.flp.FlpProject

enum class FlmLayout { ARRANGEMENT, SEQUENTIAL }

data class FlmConversion(
    val project: FlmProject,
    val layout: FlmLayout,
    val noteCount: Int,
    val warnings: List<String>,
) {
    val trackCount: Int get() = project.tracks.size
}

object FlpToFlmConverter {
    private const val OCTAVE = 12
    private const val WHOLE_NOTE_BEATS = 4L

    private data class PlacedNote(val position: Long, val note: FlpNote)

    fun defaultArrangement(project: FlpProject): FlpArrangement? =
        project.arrangements.firstOrNull { it.patternClips.isNotEmpty() }

    fun convert(project: FlpProject, arrangement: FlpArrangement? = defaultArrangement(project)): FlmConversion {
        val warnings = mutableListOf<String>()
        val source = arrangement?.takeIf { it.patternClips.isNotEmpty() }
        val placed = if (source != null) {
            placeFromArrangement(project, source, warnings)
        } else {
            placeSequentially(project, warnings)
        }
        val ticksPerBeat = project.ppq.toDouble()
        val writableChannels = project.channels.filter { it.kind != FlpChannelKind.AUTOMATION }
        val writableIds = writableChannels.map { it.iid }.toSet()
        val positions = placed.groupBy { it.note.channelIid }

        val orphanNotes = positions.filterKeys { it !in writableIds }.values.sumOf { it.size }
        if (orphanNotes > 0) warnings += "$orphanNotes note(s) point to a channel that can't be written and were skipped."

        var beyondLimit = 0
        var transposed = 0
        val usedNames = mutableSetOf(FlmWriter.MASTER_NAME)
        val tracks = writableChannels.map { channel ->
            val notes = positions[channel.iid].orEmpty()
                .sortedWith(compareBy({ it.position }, { it.note.key }))
                .mapNotNull { placedNote ->
                    val beats = placedNote.position / ticksPerBeat
                    if (beats >= FlmWriter.MAX_POSITION_BEATS) {
                        beyondLimit++
                        return@mapNotNull null
                    }
                    if (placedNote.note.key > FlmWriter.MAX_KEY) transposed++
                    toFlmNote(placedNote.note, beats, ticksPerBeat)
                }
            FlmTrack(uniqueName(channel.name, usedNames), notes)
        }

        if (beyondLimit > 0) {
            val limit = FlmWriter.MAX_POSITION_BEATS.toInt()
            warnings += "$beyondLimit note(s) start after beat $limit, the furthest position .flm notes can store, and were skipped."
        }
        if (transposed > 0) warnings += "$transposed note(s) were above key ${FlmWriter.MAX_KEY} and were moved down an octave."

        return FlmConversion(
            project = FlmProject(
                title = project.title,
                tempo = project.tempo,
                timeSigNumerator = project.timeSigNumerator,
                timeSigDenominator = project.timeSigDenominator,
                tracks = tracks,
            ),
            layout = if (source != null) FlmLayout.ARRANGEMENT else FlmLayout.SEQUENTIAL,
            noteCount = tracks.sumOf { it.notes.size },
            warnings = warnings,
        )
    }

    private fun placeFromArrangement(
        project: FlpProject,
        arrangement: FlpArrangement,
        warnings: MutableList<String>,
    ): List<PlacedNote> {
        val patterns = project.patterns.associateBy { it.iid }
        val skippedClips = arrangement.clips.size - arrangement.patternClips.size
        if (skippedClips > 0) warnings += "$skippedClips audio or automation clip(s) were skipped. Only pattern notes are converted."

        val placed = mutableListOf<PlacedNote>()
        var missingPatterns = 0
        for (clip in arrangement.patternClips) {
            val pattern = patterns[clip.patternIid]
            if (pattern == null) {
                missingPatterns++
                continue
            }
            val windowStart = clip.startOffset.toLong().coerceAtLeast(0L)
            val windowLength = clip.length.takeIf { it > 0L } ?: pattern.lengthTicks
            for (note in pattern.notes) {
                val offset = note.position - windowStart
                if (offset < 0L || offset >= windowLength) continue
                val length = minOf(note.length, windowLength - offset)
                placed += PlacedNote(clip.position + offset, note.copy(length = length))
            }
        }
        if (missingPatterns > 0) warnings += "$missingPatterns playlist clip(s) reference a pattern that doesn't exist."
        return placed
    }

    private fun placeSequentially(project: FlpProject, warnings: MutableList<String>): List<PlacedNote> {
        val filled = project.patterns.filter { it.notes.isNotEmpty() }
        if (filled.size > 1) warnings += "The playlist has no pattern clips, so patterns were placed one after another."
        val grid = project.ppq * WHOLE_NOTE_BEATS * project.timeSigNumerator / project.timeSigDenominator
        val placed = mutableListOf<PlacedNote>()
        var cursor = 0L
        for (pattern in filled) {
            pattern.notes.forEach { placed += PlacedNote(cursor + it.position, it) }
            val bars = (pattern.lengthTicks + grid - 1) / grid
            cursor += bars * grid
        }
        return placed
    }

    private fun toFlmNote(note: FlpNote, beats: Double, ticksPerBeat: Double): FlmNote {
        var key = note.key
        while (key > FlmWriter.MAX_KEY) key -= OCTAVE
        return FlmNote(
            positionBeats = beats,
            lengthBeats = note.length / ticksPerBeat,
            key = key,
            velocity = note.velocity,
            release = note.release,
            slide = note.slide,
        )
    }

    private fun uniqueName(name: String, used: MutableSet<String>): String {
        var candidate = name
        var suffix = 2
        while (!used.add(FlmWriter.asciiSafe(candidate).uppercase())) {
            candidate = "$name $suffix"
            suffix++
        }
        return candidate
    }
}
