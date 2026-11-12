package com.kylecorry.trail_sense.shared.logging

/** The app log. Each platform decides where entries go (e.g. Logcat and a log file on Android). */
interface Logger {
    fun debug(tag: String?, message: String, writeToFile: Boolean = true)
    fun debug(tag: String?, message: String, throwable: Throwable?, writeToFile: Boolean = true)
    fun info(tag: String?, message: String)
    fun info(tag: String?, message: String, throwable: Throwable?)
    fun warn(tag: String?, message: String)
    fun warn(tag: String?, message: String, throwable: Throwable?)
    fun error(tag: String?, message: String)
    fun error(tag: String?, message: String, throwable: Throwable?)

    /** Writes anything still buffered, e.g. before the process dies. */
    fun flush()
}
