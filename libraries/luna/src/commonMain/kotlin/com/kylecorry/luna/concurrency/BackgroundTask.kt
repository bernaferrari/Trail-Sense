package com.kylecorry.luna.concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class BackgroundTask(
    context: CoroutineContext = Dispatchers.Default,
    private val task: suspend () -> Unit
) {

    private val scope = CoroutineScope(context)
    private var job: Job? = null
    private val lock = SynchronizedObject()

    fun start() {
        synchronized(lock) {
            job?.cancel()
            job = scope.launch { task() }
        }
    }

    fun stop() {
        synchronized(lock) {
            job?.cancel()
        }
    }
}