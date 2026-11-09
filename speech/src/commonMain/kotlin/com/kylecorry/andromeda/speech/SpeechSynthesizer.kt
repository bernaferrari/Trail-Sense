package com.kylecorry.andromeda.speech

interface SpeechSynthesizer : AutoCloseable {
    /** Speaks using the given BCP 47 language tag, or the platform default. */
    fun speak(text: String, locale: String? = null)
    fun cancel()
    override fun close() = cancel()
}

/** Creates a speech engine. On Android, initialize the KSensor application context first. */
expect fun textToSpeech(): SpeechSynthesizer
