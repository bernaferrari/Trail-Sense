package com.kylecorry.andromeda.sound

/** Mono signed 16-bit PCM output. Writes are serialized and may wait for playback capacity.
 * Call [drain] before [close] to play all queued samples; close discards anything pending.
 */
public expect class PcmSpeaker(sampleRate: Int) : AutoCloseable {
    public suspend fun write(samples: ShortArray)
    public suspend fun drain()
    override fun close()
}
