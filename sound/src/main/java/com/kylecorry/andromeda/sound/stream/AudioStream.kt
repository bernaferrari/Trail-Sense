package com.kylecorry.andromeda.sound.stream

import android.media.AudioTrack

suspend fun AudioStream.writeToTrack(track: AudioTrack, onFirstBufferWrite: () -> Unit = {}) {
    reset()
    val buffer = ShortArray(track.bufferSizeInFrames)
    var hasBuffer = false
    while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
        nextBuffer(track.sampleRate, buffer)
        track.write(buffer, 0, buffer.size)
        if (!hasBuffer) {
            hasBuffer = true
            onFirstBufferWrite()
        }
    }
}