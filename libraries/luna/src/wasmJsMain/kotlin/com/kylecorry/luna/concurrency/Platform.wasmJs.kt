package com.kylecorry.luna.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default

/** Wasm runs coroutines on a single thread. */
internal actual fun availableProcessors(): Int = 1
