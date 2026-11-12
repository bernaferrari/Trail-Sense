package com.kylecorry.luna.cache

import com.kylecorry.luna.concurrency.SingleFlight
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A coroutine safe LRU cache, backed by memory
 */
class MemoryLRUCache<K, T>(
    private val size: Int? = null,
    private val duration: Duration? = null
) : Cache<K, T> {

    private val values = mutableMapOf<K, CachedValue<T>>()
    private val stateMutex = Mutex()
    private val singleFlight = SingleFlight<K, T>()

    override suspend fun get(key: K): T? {
        return getCached(key, updateLastUsed = true)?.value
    }

    override suspend fun peek(key: K): T? {
        return getCached(key, updateLastUsed = false)?.value
    }

    private suspend fun getCached(key: K, updateLastUsed: Boolean): CachedValue<T>? {
        return stateMutex.withLock {
            if (hasValidCache(key)) {
                val cached = values[key] ?: return@withLock null
                if (updateLastUsed) {
                    cached.cachedAt = Clock.System.now()
                }
                cached
            } else {
                null
            }
        }
    }

    override suspend fun put(key: K, value: T) {
        stateMutex.withLock {
            values[key] = CachedValue(value, Clock.System.now())
            removeOldest()
        }
    }

    override suspend fun getOrPut(key: K, lookup: suspend () -> T): T {
        val cached = getCached(key, updateLastUsed = true)
        if (cached != null) {
            return cached.value
        }
        return singleFlight.invoke(key) {
            val raced = getCached(key, updateLastUsed = true)
            if (raced != null) {
                return@invoke raced.value
            }

            val newValue = lookup()
            put(key, newValue)
            newValue
        }
    }

    override suspend fun invalidate(key: K) {
        stateMutex.withLock {
            values.remove(key)
        }
    }

    override suspend fun clear() {
        stateMutex.withLock {
            values.clear()
        }
    }

    private fun removeOldest() {
        if (size == null) {
            return
        }
        if (values.size <= size) {
            return
        }
        val oldest = values.minByOrNull { it.value.cachedAt }?.key ?: return
        values.remove(oldest)
    }

    private fun hasValidCache(key: K): Boolean {
        return values.containsKey(key) && !isCacheExpired(key)
    }

    private fun isCacheExpired(key: K): Boolean {
        if (duration == null) {
            return false
        }

        val time = values[key]?.cachedAt ?: return false

        val now = Clock.System.now()
        return time >= now || (now - time) > duration
    }

    private class CachedValue<T>(val value: T, var cachedAt: Instant)
}
