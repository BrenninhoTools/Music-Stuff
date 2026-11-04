package com.brenninhotools.musicstuff.android

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.brenninhotools.musicstuff.ui.AudioPlayer

private const val BYTES_PER_SAMPLE = 2

class AndroidAudioPlayer : AudioPlayer {
    private var track: AudioTrack? = null

    override fun play(samples: ShortArray, sampleRate: Int) {
        stop()
        val created = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * BYTES_PER_SAMPLE)
            .build()
        created.write(samples, 0, samples.size)
        created.play()
        track = created
    }

    override fun stop() {
        track?.let { current ->
            runCatching { current.stop() }
            current.release()
        }
        track = null
    }
}
