package com.kylecorry.andromeda.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

class SoundGenerator {

    fun getSound(
        sampleRate: Int = 64000,
        durationSeconds: Float = 1f,
        sampleGenerator: (i: Int) -> Double
    ): AudioTrack {
        // Adapted from https://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
        val sound = generatePcm16(sampleRate, durationSeconds, sampleGenerator)
        val size = sound.size / 2

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(sound.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(sound, 0, sound.size)
        track.setLoopPoints(0, size, Int.MAX_VALUE)
        return track
    }
}
