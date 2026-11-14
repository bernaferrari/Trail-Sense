package com.kylecorry.luna.concurrency

import kotlinx.coroutines.CoroutineDispatcher

/** The dispatcher for blocking IO; platforms without threads fall back to the default dispatcher. */
internal expect val ioDispatcher: CoroutineDispatcher

/** The number of processors work can run on in parallel. */
internal expect fun availableProcessors(): Int
