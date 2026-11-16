package com.kylecorry.trail_sense.shared.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine

actual val ioDispatcher: CoroutineDispatcher
    get() = Dispatchers.Default

actual fun <T> platformRunBlocking(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T {
    val scope = CoroutineScope(context)
    var outcome: Result<T>? = null
    val runner: suspend () -> T = { scope.block() }
    runner.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = context
        override fun resumeWith(result: Result<T>) {
            outcome = result
        }
    })
    return outcome?.getOrThrow()
        ?: error("Synchronous platformRunBlocking cannot suspend across event-loop ticks on WebAssembly")
}
