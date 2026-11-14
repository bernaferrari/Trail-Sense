package com.kylecorry.luna.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.experimental.ExperimentalNativeApi

internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

@OptIn(ExperimentalNativeApi::class)
internal actual fun availableProcessors(): Int = Platform.getAvailableProcessors()
