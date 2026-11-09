package com.kylecorry.andromeda.exceptions

/** Installs a crash-reporting callback and returns a handle that restores the previous handler. */
public expect fun installExceptionHook(report: (Throwable) -> Unit): AutoCloseable
