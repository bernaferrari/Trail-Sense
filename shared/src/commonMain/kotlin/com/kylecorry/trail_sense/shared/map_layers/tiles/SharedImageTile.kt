package com.kylecorry.trail_sense.shared.map_layers.tiles

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

/** Owns a tile image and controls concurrent reads, replacement, and disposal. */
interface TileImageStore<T> {
    fun hasImage(): Boolean
    fun replace(image: T?)
    fun withImage(block: (T?) -> Unit)
    fun clear(afterClear: () -> Unit)
}

/** Platform-independent loading, retry, invalidation, and fade policy for an image tile. */
open class SharedImageTile<T>(
    override val key: String,
    override val tile: Tile,
    private val images: TileImageStore<T>,
    initialState: TileState = TileState.Idle,
    private val shouldFadeIn: Boolean = true,
    val owner: String = "",
    private val errorRetryDelayMillis: Long = 10_000L,
    loadFunction: (suspend () -> T?)?
) : QueuedTile {
    private val stateLock = SynchronizedObject()
    private val loader = atomic(loadFunction)
    private val currentState = atomic(initialState)
    private val retryAfter = atomic(0L)
    private val loadingStart = atomic<Long?>(null)

    override val state: TileState get() = currentState.value
    override val retryAfterMillis: Long get() = retryAfter.value
    var loadingStartTime: Long?
        get() = loadingStart.value
        set(value) { loadingStart.value = value }

    override fun isLoadable(): Boolean = state == TileState.Idle || state == TileState.Stale ||
        (state == TileState.Error && nowMillis() >= retryAfterMillis)

    fun getAlpha(): Int {
        if (!shouldFadeIn) return 255
        return loadingStartTime?.let { startTime ->
            val elapsed = nowMillis() - startTime
            if (elapsed >= 250) 255 else {
                val t = elapsed / 250f
                (t * t * t * 255).toInt().coerceIn(0, 255)
            }
        } ?: 255
    }

    fun isFadingIn(): Boolean = getAlpha() != 255
    fun hasImage(): Boolean = images.hasImage()
    fun setLoader(load: suspend () -> T?) { loader.value = load }

    fun invalidate() {
        loader.value = null
        synchronized(stateLock) { currentState.value = TileState.Stale }
    }

    override suspend fun load() {
        var wasIdle = false
        val load = synchronized(stateLock) {
            val current = loader.value
            if (current == null) {
                currentState.value = TileState.Stale
                return
            }
            wasIdle = state == TileState.Idle
            currentState.value = TileState.Loading
            current
        }

        var hasImage = false
        var wasSuccess = false
        try {
            val image = load()
            images.replace(image)
            hasImage = image != null
            wasSuccess = true
        } catch (e: CancellationException) {
            synchronized(stateLock) { currentState.value = TileState.Stale }
            throw e
        } catch (e: Throwable) {
            e.printStackTrace()
            images.replace(null)
        }
        if (wasIdle) loadingStartTime = nowMillis()

        synchronized(stateLock) {
            if (state == TileState.Stale) return
            if (!wasSuccess) retryAfter.value = nowMillis() + errorRetryDelayMillis
            currentState.value = when {
                wasSuccess && hasImage -> TileState.Loaded
                wasSuccess -> TileState.Empty
                else -> TileState.Error
            }
        }
    }

    fun withImage(block: (T?) -> Unit) = images.withImage(block)

    fun recycle() {
        images.clear { synchronized(stateLock) { currentState.value = TileState.Idle } }
    }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
