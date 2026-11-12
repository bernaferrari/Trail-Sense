package com.kylecorry.luna.concurrency.locks

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class StripedLock(stripes: Int = DEFAULT_STRIPES) : Lock {

    private val locks: Array<SynchronizedObject>

    init {
        require(stripes > 0) { "stripes must be greater than 0" }
        locks = Array(stripes) { SynchronizedObject() }
    }

    override fun <T> withLock(key: Any?, block: () -> T): T {
        return synchronized(getLock(key)) {
            block()
        }
    }

    private fun getLock(key: Any?): SynchronizedObject {
        val hash = key?.hashCode() ?: 0
        return locks[hash.mod(locks.size)]
    }

    companion object {
        private const val DEFAULT_STRIPES = 64
    }
}
