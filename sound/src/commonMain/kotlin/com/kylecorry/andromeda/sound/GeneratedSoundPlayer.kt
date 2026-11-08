package com.kylecorry.andromeda.sound

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays a generated waveform until stopped. Control methods are safe to call concurrently;
 * playback errors are delivered to [scope]'s coroutine exception handler.
 */
class GeneratedSoundPlayer(
    private val scope: CoroutineScope,
    private val sampleRate: Int = 44100,
    private val sample: (Long) -> Double,
) : ISoundPlayer {
    private val lock = SynchronizedObject()
    private val volume = atomic(1f)
    private var playback: Job? = null
    private var previousPlayback: Job? = null
    private var fade: Job? = null
    private var released = false

    init { require(sampleRate > 0) }

    override fun on() = start(fadeIn = false)
    override fun fadeOn() = start(fadeIn = true)

    private fun start(fadeIn: Boolean) {
        val jobs = synchronized(lock) {
            check(!released) { "Sound player is released" }
            if (playback != null) return
            volume.value = if (fadeIn) 0f else 1f
            val previous = previousPlayback
            val job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    previous?.join()
                    playGeneratedSound(sampleRate) { index -> sample(index) * volume.value }
                } finally {
                    val current = currentCoroutineContext()[Job]
                    synchronized(lock) { if (playback === current) playback = null }
                }
            }
            playback = job
            job.invokeOnCompletion { synchronized(lock) { if (playback === job) playback = null } }
            val fading = if (fadeIn) newFadeLocked(1f, false, false) else null
            job to fading
        }
        jobs.first.start()
        jobs.second?.start()
    }

    override fun isOn(): Boolean = synchronized(lock) { playback != null }

    override fun off() {
        val jobs = synchronized(lock) {
            val oldPlayback = playback
            playback = null
            if (oldPlayback != null) previousPlayback = oldPlayback
            val oldFade = fade
            fade = null
            oldPlayback to oldFade
        }
        jobs.second?.cancel()
        jobs.first?.cancel()
    }

    override fun fadeOff(releaseWhenOff: Boolean) {
        val job = synchronized(lock) {
            if (playback == null) return
            newFadeLocked(0f, true, releaseWhenOff)
        }
        job.start()
    }

    override fun setVolume(volume: Float) {
        require(volume.isFinite())
        val oldFade = synchronized(lock) {
            check(!released) { "Sound player is released" }
            val old = fade
            fade = null
            this.volume.value = volume.coerceIn(0f, 1f)
            old
        }
        oldFade?.cancel()
    }

    override fun release() {
        synchronized(lock) { released = true }
        off()
    }

    private fun newFadeLocked(target: Float, stopAfter: Boolean, releaseAfter: Boolean): Job {
        fade?.cancel()
        val startVolume = volume.value
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val current = currentCoroutineContext()[Job]
                repeat(10) { step ->
                    delay(20)
                    synchronized(lock) {
                        if (fade !== current) return@launch
                        volume.value = startVolume + (target - startVolume) * (step + 1) / 10f
                    }
                }
                val stopped = synchronized(lock) {
                    if (stopAfter && fade === current) {
                        if (releaseAfter) released = true
                        val old = playback
                        playback = null
                        if (old != null) previousPlayback = old
                        fade = null
                        old
                    } else null
                }
                stopped?.cancel()
            } finally {
                val current = currentCoroutineContext()[Job]
                synchronized(lock) { if (fade === current) fade = null }
            }
        }
        fade = job
        return job
    }
}

/** Creates a continuous sine-tone player. */
fun tonePlayer(scope: CoroutineScope, frequencyHz: Double, sampleRate: Int = 44100): GeneratedSoundPlayer {
    require(sampleRate > 0 && frequencyHz.isFinite() && frequencyHz >= 0)
    val cyclesPerSample = (frequencyHz % sampleRate) / sampleRate
    return GeneratedSoundPlayer(scope, sampleRate) { index -> sin(2.0 * PI * ((index * cyclesPerSample) % 1.0)) }
}
