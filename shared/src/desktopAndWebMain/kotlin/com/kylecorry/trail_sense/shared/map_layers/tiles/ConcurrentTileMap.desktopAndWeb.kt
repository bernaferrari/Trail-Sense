package com.kylecorry.trail_sense.shared.map_layers.tiles

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal actual class ConcurrentTileMap<T : Any> actual constructor() {
    private val lock = SynchronizedObject()
    private val values = mutableMapOf<String, T>()

    actual operator fun get(key: String): T? = synchronized(lock) { values[key] }

    actual fun put(key: String, value: T) {
        synchronized(lock) { values[key] = value }
    }

    actual fun removeIfSame(key: String, value: T): Boolean = synchronized(lock) {
        if (values[key] != value) {
            false
        } else {
            values.remove(key)
            true
        }
    }
}
