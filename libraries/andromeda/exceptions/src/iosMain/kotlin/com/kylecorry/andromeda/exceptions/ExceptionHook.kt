package com.kylecorry.andromeda.exceptions

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.getUnhandledExceptionHook
import kotlin.native.setUnhandledExceptionHook
import kotlin.native.terminateWithUnhandledException

@OptIn(ExperimentalNativeApi::class)
public actual fun installExceptionHook(report: (Throwable) -> Unit): AutoCloseable {
    val previous = getUnhandledExceptionHook()
    val handler: (Throwable) -> Unit = { error ->
        try { report(error) } finally {
            if (previous != null) previous(error) else terminateWithUnhandledException(error)
        }
    }
    setUnhandledExceptionHook(handler)
    return AutoCloseable {
        if (getUnhandledExceptionHook() === handler) setUnhandledExceptionHook(previous)
    }
}
