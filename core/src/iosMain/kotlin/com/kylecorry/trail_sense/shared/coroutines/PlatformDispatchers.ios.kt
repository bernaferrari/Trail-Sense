package com.kylecorry.trail_sense.shared.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

actual val ioDispatcher: CoroutineDispatcher
    get() = Dispatchers.IO

actual fun <T> platformRunBlocking(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T = runBlocking(context, block)
