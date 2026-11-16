package com.kylecorry.trail_sense.shared.logging

/** Cross-platform [Logger] that writes formatted entries to standard output. */
class ConsoleLogger : Logger {
    override fun debug(tag: String?, message: String, writeToFile: Boolean) {
        log("DEBUG", tag, message, null)
    }

    override fun debug(tag: String?, message: String, throwable: Throwable?, writeToFile: Boolean) {
        log("DEBUG", tag, message, throwable)
    }

    override fun info(tag: String?, message: String) {
        log("INFO", tag, message, null)
    }

    override fun info(tag: String?, message: String, throwable: Throwable?) {
        log("INFO", tag, message, throwable)
    }

    override fun warn(tag: String?, message: String) {
        log("WARN", tag, message, null)
    }

    override fun warn(tag: String?, message: String, throwable: Throwable?) {
        log("WARN", tag, message, throwable)
    }

    override fun error(tag: String?, message: String) {
        log("ERROR", tag, message, null)
    }

    override fun error(tag: String?, message: String, throwable: Throwable?) {
        log("ERROR", tag, message, throwable)
    }

    override fun flush() = Unit

    private fun log(level: String, tag: String?, message: String, throwable: Throwable?) {
        val prefix = if (tag.isNullOrBlank()) "[$level]" else "[$level/$tag]"
        println("$prefix $message")
        if (throwable != null) {
            println(throwable.stackTraceToString())
        }
    }
}
