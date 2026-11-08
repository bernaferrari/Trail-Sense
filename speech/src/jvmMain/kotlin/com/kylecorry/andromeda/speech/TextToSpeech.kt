package com.kylecorry.andromeda.speech

import java.io.IOException
import java.util.Locale

/** Uses the desktop operating system's installed speech synthesizer. */
class TextToSpeech : ITextToSpeech {
    private val lock = Any()
    private var process: Process? = null

    override fun speak(text: String, locale: String?) {
        val started = synchronized(lock) {
            process?.destroy()
            process = null
            startSpeechProcess(locale).also { process = it }
        }
        Thread({
            try {
                started.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
                started.waitFor()
            } catch (error: IOException) {
                if (synchronized(lock) { process === started }) throw error
            } finally {
                synchronized(lock) { if (process === started) process = null }
            }
        }, "andromeda-speech").apply { isDaemon = true }.start()
    }

    override fun cancel() {
        synchronized(lock) {
            process?.destroy()
            process = null
        }
    }
}

private data class MacVoice(val name: String, val language: String)

private val macVoices: List<MacVoice> by lazy {
    runCatching {
        val process = ProcessBuilder("say", "-v", "?").redirectErrorStream(true).start()
        val expression = Regex("^(.+?)\\s{2,}([A-Za-z]{2,3}[_-][A-Za-z0-9_-]+)\\s+#.*$")
        val voices = process.inputStream.bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                expression.matchEntire(line)?.let { MacVoice(it.groupValues[1].trim(), it.groupValues[2].replace('_', '-')) }
            }.toList()
        }
        process.waitFor()
        voices
    }.getOrDefault(emptyList())
}

private fun startSpeechProcess(locale: String?): Process {
    val os = System.getProperty("os.name").lowercase(Locale.ROOT)
    val commands: List<List<String>>
    val environment = mutableMapOf<String, String>()
    when {
        os.contains("mac") -> {
            val tag = locale?.replace('_', '-')
            val voice = if (tag == null) null else macVoices.firstOrNull { it.language.equals(tag, true) }
                ?: macVoices.firstOrNull { it.language.substringBefore('-').equals(tag.substringBefore('-'), true) }
            commands = listOf(listOf("say") + if (voice == null) emptyList() else listOf("-v", voice.name))
        }
        os.contains("win") -> {
            if (locale != null) environment["ANDROMEDA_SPEECH_LOCALE"] = locale
            val script = """
                Add-Type -AssemblyName System.Speech
                [Console]::InputEncoding = [System.Text.Encoding]::UTF8
                ${'$'}speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer
                if (${'$'}env:ANDROMEDA_SPEECH_LOCALE) {
                    try {
                        ${'$'}culture = [System.Globalization.CultureInfo]::GetCultureInfo(${'$'}env:ANDROMEDA_SPEECH_LOCALE)
                        ${'$'}speaker.SelectVoiceByHints([System.Speech.Synthesis.VoiceGender]::NotSet, [System.Speech.Synthesis.VoiceAge]::NotSet, 0, ${'$'}culture)
                    } catch { }
                }
                ${'$'}speaker.Speak([Console]::In.ReadToEnd())
                ${'$'}speaker.Dispose()
            """.trimIndent()
            commands = listOf(listOf("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script))
        }
        os.contains("linux") -> {
            val arguments = (if (locale == null) emptyList() else listOf("-v", locale)) + "--stdin"
            commands = listOf(listOf("espeak-ng") + arguments, listOf("espeak") + arguments)
        }
        else -> throw UnsupportedOperationException("Text-to-speech is not supported on $os")
    }
    var lastError: IOException? = null
    for (command in commands) {
        try {
            val builder = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD)
            builder.environment().putAll(environment)
            return builder.start()
        } catch (error: IOException) {
            lastError = error
        }
    }
    throw IOException("No supported text-to-speech engine is available", lastError)
}
