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

/** App details supplied by the host application. */
class AppDetailsReport(private val name: String, private val version: String, private val debug: Boolean = false) : BugReportGenerator {
    override fun generate(error: Throwable): String = "$name $version${if (debug) " (Debug)" else ""}"
}

/** Captures device details when constructed, so generating a report need not access UI APIs. */
class DeviceDetailsReport(private val device: com.kylecorry.andromeda.core.system.DeviceInfo = com.kylecorry.andromeda.core.system.deviceInfo()) : BugReportGenerator {
    override fun generate(error: Throwable): String = buildString {
        append("Platform: ${device.platform} ${device.operatingSystemVersion}")
        device.manufacturer?.let { append("\nManufacturer: $it") }
        device.model?.let { append("\nDevice: $it") }
    }
}
