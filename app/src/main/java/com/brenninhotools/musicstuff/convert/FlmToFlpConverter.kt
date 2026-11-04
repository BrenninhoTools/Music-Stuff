package com.brenninhotools.musicstuff.convert

import com.brenninhotools.musicstuff.flm.FlmDocument
import com.brenninhotools.musicstuff.flm.FlmNote
import com.brenninhotools.musicstuff.flp.FlpArrangement
import com.brenninhotools.musicstuff.flp.FlpChannel
import com.brenninhotools.musicstuff.flp.FlpChannelKind
import com.brenninhotools.musicstuff.flp.FlpClip
import com.brenninhotools.musicstuff.flp.FlpNote
import com.brenninhotools.musicstuff.flp.FlpPattern
import com.brenninhotools.musicstuff.flp.FlpProject
import kotlin.math.roundToLong

object FlmToFlpConverter {
    const val PPQ = 384
    private const val MAX_KEY = 131
    private const val ARRANGEMENT_NAME = "Arrangement"

    fun convert(document: FlmDocument): FlpProject {
        val channels = document.channels.filter { it.clips.isNotEmpty() }
        val patterns = mutableListOf<FlpPattern>()
        val clips = mutableListOf<FlpClip>()
        channels.forEachIndexed { channelIndex, channel ->
            channel.clips.forEachIndexed { clipIndex, clip ->
                val iid = patterns.size + 1
                patterns += FlpPattern(
                    iid = iid,
                    name = "${channel.name} ${clipIndex + 1}",
                    notes = clip.notes.sortedBy { it.positionBeats }.map { toFlpNote(it, channelIndex) },
                )
                clips += FlpClip(
                    position = ticks(clip.positionBeats),
                    length = ticks(clip.lengthBeats),
                    patternIid = iid,
                    startOffset = 0f,
                    track = channelIndex,
                )
            }
        }
        return FlpProject(
            title = document.title,
            version = "",
            ppq = PPQ,
            tempo = document.tempo,
            timeSigNumerator = document.timeSigNumerator,
            timeSigDenominator = document.timeSigDenominator,
            channels = channels.mapIndexed { index, channel ->
                FlpChannel(index, channel.name, FlpChannelKind.SAMPLER)
            },
            patterns = patterns,
            arrangements = listOf(FlpArrangement(0, ARRANGEMENT_NAME, clips)),
            warnings = document.warnings,
        )
    }

    private fun ticks(beats: Double): Long = (beats * PPQ).roundToLong()

    private fun toFlpNote(note: FlmNote, channel: Int) = FlpNote(
        position = ticks(note.positionBeats),
        length = ticks(note.lengthBeats).coerceAtLeast(1L),
        key = note.key.coerceIn(0, MAX_KEY),
        velocity = note.velocity,
        release = note.release,
        channelIid = channel,
        slide = false,
    )
}
