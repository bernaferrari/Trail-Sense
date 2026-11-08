package com.kylecorry.andromeda.exceptions

fun interface BugReportGenerator {
    fun generate(error: Throwable): String
}

object StackTraceReport : BugReportGenerator {
    override fun generate(error: Throwable): String =
        "Message: ${error.message.orEmpty()}\n\n${error.stackTraceToString()}"
}

class CombinedBugReport(private val generators: List<BugReportGenerator>) : BugReportGenerator {
    override fun generate(error: Throwable): String = generators.joinToString("\n") { generator ->
        runCatching { generator.generate(error) }.getOrDefault("")
    }
}
