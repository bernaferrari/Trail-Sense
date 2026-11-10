package com.kylecorry.andromeda.core.cache

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * A cache which stores values based on their location
 * @param cacheRadius The radius for each cache key
 * @param size The maximum number of values to store in the cache, defaults to unlimited
 * @param cacheDuration The maximum duration to store values in the cache, defaults to forever
 * @param removalStrategy The strategy to use when removing values from the cache, defaults to furthest
 */
public class GeospatialCache<T>(
    cacheRadius: Distance,
    private val size: Int? = null,
    private val cacheDuration: Duration? = null,
    private val removalStrategy: RemovalStrategy = RemovalStrategy.Furthest,
    private val timeProvider: () -> Instant = { Clock.System.now() }
) {

    private val values: MutableMap<Coordinate, T> = mutableMapOf()
    private val lastAccessed: MutableMap<Coordinate, Instant> = mutableMapOf()
    private val cachedAt: MutableMap<Coordinate, Instant> = mutableMapOf()
    private var mutex = Mutex()
    private val cacheRadiusMeters = cacheRadius.meters().value

    /**
     * Gets the value at the given location if it exists and is valid, otherwise null
     * @param location The location to get the value at
     * @return The value or null
     */
    public suspend fun get(location: Coordinate): T? {
        return mutex.withLock {
            getKeyLocation(location)?.let {
                lastAccessed[it] = timeProvider()
                values[it]
            }
        }
    }

    public suspend fun getAll(locations: List<Coordinate>): Map<Coordinate, T> {
        return mutex.withLock {
            val results = mutableMapOf<Coordinate, T>()
            for (location in locations) {
                getKeyLocation(location)?.let { key ->
                    lastAccessed[key] = timeProvider()
                    values[key]?.let { value ->
                        results[location] = value
                    }
                }
            }
            results
        }
    }

    /**
     * Puts a value at the given location
     * @param location The location to put the value at
     * @param value The value to put
     */
    public suspend fun put(location: Coordinate, value: T) {
        mutex.withLock {
            val key = getKeyLocation(location) ?: location
            values[key] = value
            lastAccessed[key] = timeProvider()
            cachedAt[key] = timeProvider()
            cleanup(location)
        }
    }

    public suspend fun putAll(locations: Map<Coordinate, T>) {
        mutex.withLock {
            for ((location, value) in locations) {
                val key = getKeyLocation(location) ?: location
                values[key] = value
                lastAccessed[key] = timeProvider()
                cachedAt[key] = timeProvider()
            }
            cleanup(locations.keys.firstOrNull() ?: Coordinate(0.0, 0.0))
        }
    }

    /**
     * Gets the value at the given location if it exists and is valid,
     * otherwise puts the result of the lookup function at the location
     * @param location The location to get the value at
     * @param lookup The lookup function
     * @return The value
     */
    public suspend fun getOrPut(location: Coordinate, lookup: suspend () -> T): T {
        return mutex.withLock {
            val key = getKeyLocation(location) ?: location
            if (values.containsKey(key)) {
                lastAccessed[key] = timeProvider()
                return@withLock values[key]!!
            }
            val newValue = lookup()
            values[key] = newValue
            lastAccessed[key] = timeProvider()
            cachedAt[key] = timeProvider()
            cleanup(location)
            newValue
        }
    }

    /**
     * Invalidates the value at the given location
     * @param location The location to invalidate
     */
    public suspend fun invalidate(location: Coordinate) {
        mutex.withLock {
            getKeyLocation(location)?.let {
                values.remove(it)
                lastAccessed.remove(it)
                cachedAt.remove(it)
            }
        }
    }

    /**
     * Invalidates all values in the cache
     */
    public suspend fun invalidateAll() {
        mutex.withLock {
            values.clear()
            lastAccessed.clear()
            cachedAt.clear()
        }
    }

    /**
     * Get the key location for the given location. This will always be a valid key in the cache.
     * @param location The location to get the key location for
     * @return The key location or null if no valid key exists
     */
    private fun getKeyLocation(location: Coordinate): Coordinate? {
        // Get the cache key that is within the cache radius to the given location and is valid
        val validKeys =
            values.keys.filter { !isExpired(it) && it.distanceTo(location) <= cacheRadiusMeters }
        if (validKeys.isEmpty()) {
            return null
        }

        return validKeys.minByOrNull { it.distanceTo(location) }
    }

    private fun cleanup(location: Coordinate) {
        // Remove expired values
        val valuesToRemove = values.filter { isExpired(it.key) }.map { it.key }.toMutableList()

        // If the cache is still too big, remove the least recently used or furthest value
        if (size != null && (values.size - valuesToRemove.size) > size) {
            val toRemove = when (removalStrategy) {
                RemovalStrategy.LeastRecentlyUsed -> getLeastRecentlyUsed()
                RemovalStrategy.Furthest -> getFurthest(location)
            }
            toRemove?.let { valuesToRemove.add(it) }
        }

        // Remove the values
        valuesToRemove.forEach {
            values.remove(it)
            lastAccessed.remove(it)
            cachedAt.remove(it)
        }
    }

    private fun getLeastRecentlyUsed(): Coordinate? {
        return lastAccessed.minByOrNull { it.value }?.key
    }

    private fun getFurthest(location: Coordinate): Coordinate? {
        return values.keys.maxByOrNull { it.distanceTo(location) }
    }

    private fun isExpired(key: Coordinate): Boolean {
        if (cacheDuration == null) {
            return false
        }
        val cachedAtTime = cachedAt[key] ?: return false
        return timeProvider() - cachedAtTime > cacheDuration
    }

    public enum class RemovalStrategy {
        LeastRecentlyUsed,
        Furthest
    }

}