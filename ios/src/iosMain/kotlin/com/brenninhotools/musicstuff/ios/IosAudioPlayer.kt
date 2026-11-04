package com.brenninhotools.musicstuff.ios

import com.brenninhotools.musicstuff.generate.PreviewSynth
import com.brenninhotools.musicstuff.ui.AudioPlayer
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive

class IosAudioPlayer : AudioPlayer {
    private var player: AVAudioPlayer? = null

    override fun play(samples: ShortArray, sampleRate: Int) {
        stop()
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
        val created = AVAudioPlayer(data = PreviewSynth.wav(samples).toNSData(), error = null)
        check(created.play())
        player = created
    }

    override fun stop() {
        player?.stop()
        player = null
    }
}
