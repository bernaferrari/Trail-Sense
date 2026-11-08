package com.kylecorry.andromeda.exceptions

actual fun installExceptionHook(report: (Throwable) -> Unit): AutoCloseable {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    val handler = Thread.UncaughtExceptionHandler { thread, error ->
        try { report(error) } finally {
            if (previous != null) previous.uncaughtException(thread, error) else error.printStackTrace()
        }
    }
    Thread.setDefaultUncaughtExceptionHandler(handler)
    return AutoCloseable {
        if (Thread.getDefaultUncaughtExceptionHandler() === handler) Thread.setDefaultUncaughtExceptionHandler(previous)
    }
}
