package com.brenninhotools.musicstuff.desktop

import com.brenninhotools.musicstuff.ui.AudioPlayer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineUnavailableException

private const val BITS_PER_SAMPLE = 16
private const val BYTES_PER_SAMPLE = 2
private const val MONO = 1

class DesktopAudioPlayer : AudioPlayer {
    private var clip: Clip? = null

    override fun play(samples: ShortArray, sampleRate: Int) {
        stop()
        val buffer = ByteBuffer.allocate(samples.size * BYTES_PER_SAMPLE).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { buffer.putShort(it) }
        val format = AudioFormat(sampleRate.toFloat(), BITS_PER_SAMPLE, MONO, true, false)
        try {
            val created = AudioSystem.getClip()
            created.open(format, buffer.array(), 0, buffer.capacity())
            created.start()
            clip = created
        } catch (error: LineUnavailableException) {
            throw IllegalStateException(error)
        }
    }

    override fun stop() {
        clip?.let { current ->
            current.stop()
            current.close()
        }
        clip = null
    }
}
